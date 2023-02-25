from typing import Tuple, List, Any
import torch
import numpy as np
import logging
import onnxruntime

from .audio_processing import to_numpy, FeaturizedAudioDataset
from .my_constants import VAD_CONF, VAD_ONNX_FILENAME, VAD_POST_PROCESSING_PER_ARGS
from .nemo_vad_util import generate_vad_segment_table_per_tensor

try:
    from torch.cuda.amp import autocast
except ImportError:
    from contextlib import contextmanager

    @contextmanager
    def autocast(enabled=None):
        yield


#spokens: List[Tuple[int, int, Any]] = run_voice_activity_detection(
#        waveform = video_waveform, 
#        vad_featurizer = vad_f,
#        vad_session = vad_session
#    )
def run_voice_activity_detection(
    waveform: np.ndarray, 
    vad_featurizer: torch.nn.Module, 
    vad_session: onnxruntime.InferenceSession
) -> List[Tuple[int, int, Any]]:
    data_layer = FeaturizedAudioDataset(
            signals = [], 
            featurizer = vad_featurizer, 
            normalize=False
        )

    logging.debug("init_data_layer OK")

    # infer vad 
    result = offline_inference_raw(
            waveform = waveform, 
            data_layer=data_layer, 
            vad_session=vad_session,
            verbose=True
        )
    logging.debug("vad inference OK")

    # post processing
    preds = generate_vad_segment_table_per_tensor(torch.tensor(result[2]), VAD_POST_PROCESSING_PER_ARGS)
    spokens = [(start.detach().item(), end.detach().item(), duration.detach().item()) for start, end, duration in preds]
    spokens = [(int(start*16000.0), int(end*16000.0), duration) for start, end, duration in spokens]
    
    window_size = 16000*5
    spokens = [ 
        [(start+dx, min(start+dx+window_size, end), duration) for dx in range(start, end, window_size)]  
        for start, end, duration in spokens
    ]
    
    # flatten
    spokens = [ spoken for spoken_list in spokens for spoken in spoken_list ]
    
    # filtering
    spokens = [ (start, end, duration)  for start, end, duration in spokens if start < end]

    return spokens

# threshold =        0.5
# STEP_LIST =        [0.01,0.01, 0.08, 0.025]
# WINDOW_SIZE_LIST = [0.31,0.15, 0.63, 0.5]
def offline_inference_raw(
        waveform,
        STEP = 0.01,
        WINDOW_SIZE = 0.63,
        threshold=0.5,
        SAMPLE_RATE=16000,
        data_layer=None,
        vad_session=None,
        verbose=False
    ):
    assert data_layer is not None and vad_session is not None
    assert SAMPLE_RATE == 16000

    waveform = waveform.copy()

    FRAME_LEN = STEP # infer every STEP seconds
    CHANNELS = 1 # number of audio channels (expect mono signal)
    RATE = 16000 # sample rate, Hz
    CHUNK_SIZE = int(FRAME_LEN*RATE)
    FRAME_OVERLAP = (WINDOW_SIZE-FRAME_LEN)/2

    frame_vad = FrameVAD(
            model_definition = {
                'sample_rate': SAMPLE_RATE,
                'preprocessor_window_stride': VAD_CONF['window_stride'],
                'strides_repeats': VAD_CONF['strides_repeats'],
                'labels': VAD_CONF['labels']
            },
            threshold=threshold,
            frame_len=FRAME_LEN,
            frame_overlap = FRAME_OVERLAP,
            offset=0
        )
    
    # if is_librosa_waveform:
    #    waveform *= 2 ** (16-1)
    #    waveform = waveform.astype(np.int16)

    empty_counter = 0
    preds = []
    proba_b = []
    proba_s = []
    wf_size = waveform.size
    offset = 0

    while offset < wf_size:
        signal = waveform[offset:offset+CHUNK_SIZE]
        offset += CHUNK_SIZE
        result = frame_vad.transcribe(
                frame = signal,
                data_layer = data_layer,
                vad_session = vad_session
            )
        preds.append(result[0])
        proba_b.append(result[2])
        proba_s.append(result[3])
        if len(result):
            if verbose:
                print(result,end='\n')
            empty_counter = 3
        elif empty_counter > 0:
            empty_counter -= 1
            if empty_counter == 0:
                if verbose:
                    print(' ',end='')

    frame_vad.reset()
    return preds, proba_b, proba_s


# inference method for audio signal (single instance)
def _infer_signal(data_layer = None, vad_session = None):
    assert data_layer is not None and vad_session is not None
    batch = next(iter(data_layer))

    audio_signal, audio_signal_len = batch
    processed_signal = audio_signal.detach()
    processed_signal_len = audio_signal_len.detach()

    ort_inputs = { vad_session.get_inputs()[0].name: to_numpy(processed_signal), }
    ologits = vad_session.run(None, ort_inputs)
    alogits = np.asarray(ologits)
    logits = torch.from_numpy(alogits[0])
    return logits

# https://github.com/NVIDIA/NeMo/blob/main/tutorials/asr/Online_Offline_Microphone_VAD_Demo.ipynb
# class for streaming frame-based VAD
# 1) use reset() method to reset FrameVAD's state
# 2) call transcribe(frame) to do VAD on
#    contiguous signal's frames
# To simplify the flow, we use single threshold to binarize predictions.
class FrameVAD:
    def __init__(self, 
                 model_definition,
                 threshold, #=0.5,
                 frame_len, #=2, 
                 frame_overlap, #=2.5,
                 offset # =10
            ):
        '''
        Args:
          threshold: If prob of speech is larger than threshold, classify the segment to be speech.
          frame_len: frame's duration, seconds
          frame_overlap: duration of overlaps before and after current frame, seconds
          offset: number of symbols to drop for smooth streaming
        '''
        self.vocab = list(model_definition['labels'])
        self.vocab.append('_')
        self.sr = model_definition['sample_rate']
        self.threshold = threshold
        self.frame_len = frame_len
        self.n_frame_len = int(frame_len * self.sr)
        self.frame_overlap = frame_overlap
        self.n_frame_overlap = int(frame_overlap * self.sr)
        timestep_duration = model_definition['preprocessor_window_stride']
        strides_repeats = model_definition['strides_repeats']

        for block in strides_repeats:
            timestep_duration *= block[0] ** block[1]
        self.buffer = np.zeros(shape=2*self.n_frame_overlap + self.n_frame_len,
                               dtype=np.float32)
        self.offset = offset
        self.reset()
        
    def _decode(self, frame, offset=0, vad_session=None, data_layer=None):
        assert len(frame)==self.n_frame_len
        assert data_layer is not None and vad_session is not None

        self.buffer[:-self.n_frame_len] = self.buffer[self.n_frame_len:]
        self.buffer[-self.n_frame_len:] = frame
        data_layer._signals = [self.buffer]

        logits = _infer_signal(
                data_layer = data_layer,
                vad_session = vad_session
            ).detach().numpy()[0]

        decoded = self._greedy_decoder(
            self.threshold,
            logits,
            self.vocab
        )
        return decoded

    @torch.no_grad()
    def transcribe(
            self, 
            frame=None, 
            vad_session=None,
            data_layer=None
        ):
        assert data_layer is not None and vad_session is not None
        if frame is None:
            frame = np.zeros(shape=self.n_frame_len, dtype=np.float32)
        if len(frame) < self.n_frame_len:
            frame = np.pad(frame, [0, self.n_frame_len - len(frame)], 'constant')
        unmerged = self._decode(
            frame = frame, 
            offset = self.offset, 
            vad_session = vad_session,
            data_layer = data_layer
        )
        return unmerged

    def reset(self):
        '''
        Reset frame_history and decoder's state
        '''
        self.buffer=np.zeros(shape=self.buffer.shape, dtype=np.float32)
        self.prev_char = ''
    
    @staticmethod
    def _greedy_decoder(threshold, logits, vocab):
        s = []
        if logits.shape[0]:
            probs = torch.softmax(torch.as_tensor(logits), dim=-1)
            probas, _ = torch.max(probs, dim=-1)
            probas_s = probs[1].item()
            preds = 1 if probas_s >= threshold else 0
            s = [preds, str(vocab[preds]), probs[0].item(), probs[1].item(), str(logits)]
        return s


