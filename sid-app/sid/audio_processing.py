import io
import logging
import torch
import librosa
import scipy
from typing import Optional
import random
import numpy as np
import torchaudio
from torch.utils.data.dataset import IterableDataset
from pydub import AudioSegment
import wave

from .nemo_audio_processing_util import FilterbankFeatures

def join_wav_blobs(wav_blobs):
    if len(wav_blobs) == 1:
        return wav_blobs[0]
    
    data = []
    for wav_blob in wav_blobs:
        bo = io.BytesIO(wav_blob)
        w = wave.open(bo, "rb")
        
        data.append([ w.getparams(), w.readframes(w.getnframes()) ])
        
        w.close()
        bo.close()
    
    output_bo = io.BytesIO()
    output_w = wave.open(output_bo, "wb")
    output_w.setparams(data[0][0])
    for i in range(len(data)):
        output_w.writeframes(data[i][1])
    output_w.close()
    output_bo.seek(0)
    output_blob = output_bo.read()
    output_bo.close()

    return output_blob

def to_sliced_audio_segments(
    audio_blob: bytes, 
    slicing_ms: int = 1000*60*5 # 5 MIN
) -> AudioSegment:
    bo = io.BytesIO(audio_blob)
    full_audio = AudioSegment(bo)
    bo.close()
    
    full_audio_size = len(full_audio)
    audio_segments = [ 
        full_audio[s:s+slicing_ms]
        for s 
        in range(0, full_audio_size, slicing_ms)
    ]

    return audio_segments
 

def load_waveform(blob: bytes) -> Optional[torch.Tensor]:
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

def load_numpy_waveform_using_librosa(blob: bytes) -> np.ndarray:
    bo = io.BytesIO(blob)
    y, sr = librosa.load(bo, sr=16000)
    bo.close()
    assert sr == 16000
    return y

def load_numpy_waveform(blob: bytes) -> np.ndarray:
    bo = io.BytesIO(blob)
    sr, audio = scipy.io.wavfile.read(bo)
    bo.close()

    assert sr == 16000

    audio = audio.astype("float32") / 32768.0
    return audio


