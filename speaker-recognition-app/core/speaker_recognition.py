from typing import Optional, Tuple, Any, Dict, List
import io
# from .my_constants import TDNN_MODEL_FILENAME, PRETRAINED_TDNN_MODEL_NAME
# from speechbrain.pretrained import SpeakerRecognition
import torch
#torch.set_num_threads = 2
#torch.set_num_interop_threads = 4
torch.set_grad_enabled(False)

from torch import Tensor
import torchaudio
# import nemo.collections.asr as nemo_asr
import onnxruntime
import logging
import sys

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

def load_onnx_session(onnx_filename):
    return onnxruntime.InferenceSession(onnx_filename)

def to_numpy(tensor):
    return tensor.detach().cpu().numpy() if tensor.requires_grad else tensor.cpu().numpy()

def get_embedding_using_onnx(waveform, onnx_session, featurizer):
    processed_signal, processed_len = featurizer(waveform, torch.tensor([waveform.squeeze().shape[0]]))
    ort_inputs = {
        onnx_session.get_inputs()[0].name: to_numpy(processed_signal), 
        onnx_session.get_inputs()[1].name: to_numpy(processed_len)
    }
    logits, embedding = onnx_session.run(None, ort_inputs)
    return torch.tensor(embedding)

def load_model():
    assert False
    # try:
        # model = nemo_asr.models.EncDecSpeakerLabelModel.restore_from(TDNN_MODEL_FILENAME)
    # except FileNotFoundError:
    # model = nemo_asr.models.EncDecSpeakerLabelModel.from_pretrained(PRETRAINED_TDNN_MODEL_NAME)
    # model.save_to(TDNN_MODEL_FILENAME)
    # return model

def get_embedding(model, audio_signal, audio_signal_len):
    mode = False
    model.freeze()
    logits, emb = model.forward(input_signal=audio_signal, input_signal_length=audio_signal_len)
    model.train(mode=mode)
    return emb

def verify(emb1, emb2, threshold=0.7):
    emb1 = emb1.squeeze()
    emb2 = emb2.squeeze()

    X = emb1 / torch.linalg.norm(emb1)
    Y = emb2 / torch.linalg.norm(emb2)

    similarity_score = torch.dot(X, Y) / ((torch.dot(X, X) * torch.dot(Y, Y)) ** 0.5)
    similarity_score = (similarity_score + 1) / 2
    
    if similarity_score >= threshold:
        logging.info(" two audio files are from same speaker")
        return similarity_score, True
    else:
        logging.info(" two audio files are from different speakers")
        return similarity_score, False

def audio_blob_to_waveform(blob: bytes) -> Optional[Tensor]:
    bo = io.BytesIO(blob)
    try:
        with torch.no_grad():
            waveform, sr = torchaudio.load(bo)
            assert sr == 16000
    except RuntimeError as e:
        logging.info("audio_blob_to_waveform FAIL!!! ERRROR")
        logging.info(e)
        return None
    finally:
        bo.close()
        del bo

    return waveform
