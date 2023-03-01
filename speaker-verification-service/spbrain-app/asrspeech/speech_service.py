from typing import Optional, Tuple, Any, Dict, List
import io
from whisper import Whisper
from speechbrain.pretrained import SpeakerRecognition

import torch
#torch.set_num_threads = 2
#torch.set_num_interop_threads = 4
torch.set_grad_enabled(False)

from torch import Tensor
import torchaudio

class SpeechService:
    def __init__(self, embedding_model: Optional[SpeakerRecognition], transcript_model: Optional[Whisper]):
        self.embedding_model = embedding_model
        self.transcript_model = transcript_model
        if embedding_model is not None:
          self.similarity = torch.nn.CosineSimilarity(dim=-1, eps=1e-6).eval()
          self.similarity.zero_grad(set_to_none=True)
        else:
          self.similarity = None


    def transcribe(self, waveform: Optional[Tensor] = None):
        if self.transcript_model is None:
            raise RuntimeError("transcript_model is None")
        if waveform is None:
            raise RuntimeError("transcribe_file invalid use..")

        with torch.no_grad():
            result = self.transcript_model.transcribe(waveform.squeeze(), language='en', fp16=False)
        
        return result
    
    @staticmethod
    def audio_blob_to_waveform(blob: bytes) -> Optional[Tensor]:
        bo = io.BytesIO(blob)
        try:
            with torch.no_grad(): 
                waveform, sr = torchaudio.load(bo)
                assert sr == 16000
        except RuntimeError as e:
            print(e)
            return None
        finally:
            bo.close()
        return waveform

    def waveform_to_embedding(self, waveform: Tensor) -> Tensor:
        with torch.no_grad():
            embedding: Tensor = self.embedding_model.encode_batch(waveform)
        return embedding

    def verify(self, embedding1: Tensor, embedding2: Tensor) -> Tuple[Tensor, Tensor]:
        assert embedding1 is not None
        assert embedding2 is not None
        
        with torch.no_grad():
            score_tensor = self.similarity(embedding1, embedding2)
            prediction_tensor = score_tensor >= 0.5

        return score_tensor, prediction_tensor
