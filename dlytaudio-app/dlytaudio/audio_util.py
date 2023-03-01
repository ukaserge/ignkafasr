import ffmpeg
from pydub import AudioSegment

def webm_to_wav(src_webm_filename, dest_wav_filename, dest_sample_rate=16000, dest_channel=1):
    src_webm_stream = ffmpeg.input(src_webm_filename, f='webm')
    wav_stream = ffmpeg.output(src_webm_stream, dest_wav_filename, format='wav', osr=dest_sample_rate, ac=dest_channel)
    ffmpeg.run(wav_stream, overwrite_output=True)
    return dest_wav_filename

def wav_to_chunk_blob_dict(
    wav_filename, 
    prefix,
    chunk_size=40,
):
    assert wav_filename[-4:] == '.wav'

    audio = AudioSegment.from_file(wav_filename, format="wav")
    audio_size = len(audio)
    
    chunk_sequence = (audio[start:start+chunk_size] for start in range(0, audio_size, chunk_size))
    
    chunk_blob_dict = { 
        f'{prefix}{i}':  chunk.export(format='wav').read() 
        for i, chunk 
        in enumerate(chunk_sequence) 
        if len(chunk.raw_data) != 0
    }

    return chunk_blob_dict
