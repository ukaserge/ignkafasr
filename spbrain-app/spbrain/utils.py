import torchaudio
from speechbrain.pretrained import SpeakerRecognition, SepformerSeparation, VAD
from speechbrain.dataio.dataio import read_audio
from typing import Callable, Awaitable, Optional, Dict, Tuple, List, Coroutine, Any
from torch import Tensor


# async def blob_to_waveform_and_sample_rate(
#         blob: bytes
# ) -> Tuple[Tensor, int]:
#     foo = open("foo.wav", mode="wb")
#     foo.write(blob)
#     foo.close()
#
#     # noinspection PyUnresolvedReferences
#     cand_waveform, cand_sample_rate = torchaudio.load("foo.wav")
#
#     return cand_waveform, cand_sample_rate


def run_vad(vad_model: VAD, filepath: str):
    # 1- Let's compute frame-level posteriors first
    audio_file = filepath
    prob_chunks = vad_model.get_speech_prob_file(
        audio_file,
        small_chunk_size=1,
        large_chunk_size=60
    )

    # 2- Let's apply a threshold on top of the posteriors
    prob_th = vad_model.apply_threshold(
        prob_chunks,
        # activation_th=0.8,
        # deactivation_th=0.4
        # activation_th=0.7,
        # deactivation_th=0.4
    ).float()

    # 3- Let's now derive the candidate speech segments
    boundaries = vad_model.get_boundaries(prob_th)

    # 5- Merge segments that are too close
    boundaries = vad_model.merge_close_segments(boundaries, close_th=0.3)

    # 6- Remove segments that are too short
    boundaries = vad_model.remove_short_segments(boundaries, len_th=0.2)

    # 7- Double-check speech segments (optional).
    boundaries = vad_model.double_check_speech_segments(boundaries, audio_file, speech_th=0.25)

    return boundaries


def file_to_vad_segments(
        vad_model: VAD,
        file: str,
        logger: Any
) -> List[Tensor]:
    # seg = VAD.get_segments(boundaries=run_vad(file), audio_file=file)
    seg = vad_model.get_speech_segments(file, small_chunk_size=2, large_chunk_size=60)
    logger.info(seg)

    ret = []
    # seg = seg.squeeze().squeeze().squeeze()

    if len(seg) == 0:
        return []
    seg = seg.flatten()
    for s, e in seg.reshape((len(seg) // 2, 2)):
        ret.append(read_audio({
            "file": file,
            "start": int(s.item() * 16000.0),
            "stop": int(e.item() * 16000.0)
        }))
        # print_waveform(ret[-1])
    logger.info(ret)
    return ret


def wf_to_vad_segments(
        vad_model: VAD,
        wf: Tensor,
        logger: Any
) -> List[Tensor]:
    # noinspection PyUnresolvedReferences
    torchaudio.save("foo.wav", wf.unsqueeze(0), 16000)
    return file_to_vad_segments(vad_model, "foo.wav")
