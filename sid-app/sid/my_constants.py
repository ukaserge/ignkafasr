# KAFKA
KAFKA_GROUP_ID = 'sr-group'
SPEECH_REQUEST_TOPIC = 'speech.request'
ANALYSIS_RESULT_SID_TOPIC = "analysis.result.sid"

SR_ONNX_FILENAME = '/app/sid/titanet.onnx'
# VAD_ONNX_FILENAME = '/app/core/vad_multi.onnx'

# IGNITE
BLOB_CACHE = 'blobs'
KEY2NAME_CACHE = 'key2name'
EMBEDDING_CACHE = 'embeddings'

# Whisper
WHISPER_LIBNAME = '/app/whisper.cpp/libwhisper.so'
WHISPER_FNAME_MODEL = '/app/whisper.cpp/models/ggml-base.bin'

# Preprocessor Configurations
SR_CONF = {
     '_target_': 'nemo.collections.asr.modules.AudioToMelSpectrogramPreprocessor',
     'normalize': 'per_feature', 
     'window_size': 0.025, 
     'sample_rate': 16000,
     'window_stride': 0.01,
     'window': 'hann',
     'features': 80,
     'n_fft': 512,
     'frame_splicing': 1,
     'dither': 1e-05,
     'nb_augmentation_prob': 0.0
}

VAD_CONF = {
    '_target_': 'nemo.collections.asr.modules.AudioToMelSpectrogramPreprocessor',
    'normalize': 'None',
    'window_size': 0.025,
    'sample_rate': 16000,
    'window_stride': 0.01,
    'window': 'hann',
    'features': 80,
    'n_fft': 512,
    'frame_splicing': 1,
    'dither': 1e-05,
    'stft_conv': False,
    'nb_augmentation_prob': 0.5,
    'nb_max_freq': 4000,

    # [(block['stride'][0], block['repeat']) for block in vad_model.cfg.encoder['jasper']]
    'strides_repeats': [(1, 1), (1, 2), (1, 2), (1, 2), (1, 1), (1, 1)],

    # vad_model.cfg.labels
    'labels': ['background', 'speech'],
}

VAD_POST_PROCESSING_PER_ARGS = {
    'frame_length_in_sec': 0.01, 
    'onset': 0.5, 
    'offset': 0.3, 
    'min_duration_on': 0.5, 
    'min_duration_off': 0.5, 
    'filter_speech_first': 1.0, 
    'window_length_in_sec': 0.63, 
    'shift_length_in_sec': 0.01
}
