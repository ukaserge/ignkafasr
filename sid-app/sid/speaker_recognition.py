import threading
import os
from typing import Tuple, Dict
import torch
import logging

import onnxruntime
from .torch_util import to_numpy

def infer_speaker_embedding(
    waveform: torch.Tensor, 
    sr_session: onnxruntime.InferenceSession, 
    sr_featurizer: torch.nn.Module
) -> torch.Tensor:
    if len(waveform.shape) == 1:
        shape_tensor = torch.tensor([waveform.shape[0]])
        waveform = waveform.unsqueeze(0)
    elif len(waveform.shape) == 2:
        shape_tensor = torch.tensor([waveform.squeeze().shape[0]])
    else:
        logging.debug(waveform)
        assert False
    if waveform.squeeze().shape[0] == 0 or shape_tensor.shape[0] == 0:
        logging.debug(waveform)
        logging.debug(shape_tensor)
        assert False

    processed_signal, processed_len = sr_featurizer(waveform, shape_tensor)
    ort_inputs = {
            sr_session.get_inputs()[0].name: to_numpy(processed_signal), 
            sr_session.get_inputs()[1].name: to_numpy(processed_len)
        }
    logits, embedding = sr_session.run(None, ort_inputs)
    return torch.tensor(embedding)

def compare_embedding(
    emb1: torch.Tensor,
    emb2: torch.Tensor, 
    threshold=0.75
) -> Tuple[torch.Tensor, bool]:
    emb1 = emb1.squeeze()
    emb2 = emb2.squeeze()

    X = emb1 / torch.linalg.norm(emb1)
    Y = emb2 / torch.linalg.norm(emb2)

    similarity_score = torch.dot(X, Y) / ((torch.dot(X, X) * torch.dot(Y, Y)) ** 0.5)
    similarity_score = (similarity_score + 1) / 2
    
    if similarity_score >= threshold:
        logging.debug(" two audio files are from same speaker")
        return similarity_score, True
    else:
        logging.debug(" two audio files are from different speakers")
        return similarity_score, False

def search_speaker_by_embedding(
    target_embedding: torch.Tensor, 
    speakers_embedding_dict: Dict[str, torch.Tensor], 
    speakers_key2name: Dict[str, str]
) -> Tuple[str, float]:
    assert len(speakers_embedding_dict.keys()) != 0
    logging.info(
        '#%sT%s - Verify Stream Start!!',
        os.getpid(), threading.get_ident()
    )
    
    verify_stream = (
        (speaker_key, *compare_embedding(speaker_embedding, target_embedding))
        for speaker_key, speaker_embedding 
        in speakers_embedding_dict.items()
    )
    # => ( ('speaker_key', Tensor([0.7]), True)), (...), (....) )
    match_speaker_key, match_score, match_prediction = max(
        verify_stream,
        key = lambda res: res[1]
    )
    # => '{speaker_key}', Tensor({float value}), Tensor({boolean value})

    logging.info(
        '#%sT%s - Verify Stream OK!!',
        os.getpid(), threading.get_ident()
    )

    match_score = match_score.item()
    match_speaker_name = speakers_key2name[match_speaker_key] if match_score >= 0.75 else 'unknown'

    return match_speaker_name, match_score


