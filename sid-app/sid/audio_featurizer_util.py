import torch
import numpy as np
from torch.utils.data.dataset import IterableDataset

from .nemo_audio_processing_util import FilterbankFeatures

# f = load_featurizer(SR_CONF)
# wf, sr = torchaudio.load("example1.wav")
# processed_signal, processed_len = f(wf, torch.tensor([wf.squeeze().shape[0]]))
def load_featurizer(conf):
    return FilterbankFeatures(
        sample_rate=conf['sample_rate'],
        n_window_size=int(conf['window_size'] * conf['sample_rate']),
        n_window_stride=int(conf['window_stride'] * conf['sample_rate']),
        window=conf['window'],
        normalize=conf['normalize'],
        n_fft=conf['n_fft'],
        preemph=0.97,
        nfilt=conf['features'],
        lowfreq=0,
        highfreq=None,
        log=True,
        log_zero_guard_type="add",
        log_zero_guard_value=2 ** -24,
        dither=conf['dither'],
        pad_to=16,
        frame_splicing=conf['frame_splicing'],
        exact_pad=False,
        pad_value=0,
        mag_power=2.0,
        rng=None,
        nb_augmentation_prob=conf['nb_augmentation_prob'],
        nb_max_freq=4000,
        stft_exact_pad=False,  # Deprecated arguments; kept for config compatibility
        stft_conv=False,  # Deprecated arguments; kept for config compatibility
    )

class FeaturizedAudioDataset(IterableDataset):
    def __init__(self, signals, featurizer, sample_rate=16000, normalize=False):
        super().__init__()
        self._sample_rate = sample_rate
        self._featurizer = featurizer
        self._signals = signals
        self._normalize = normalize
    def __iter__(self):
        for signal in self._signals:
            if len(signal.shape) != 1:
                raise RuntimeError("expected len(signal.shape) is 1")
            signal = signal.astype(np.float32)
            signal_size = signal.size
            if self._normalize:
                signal = signal/32768.
            with torch.no_grad():
                processed_signal, processed_signal_len = self._featurizer( 
                    torch.as_tensor(signal, dtype=torch.float32).unsqueeze(0).detach(),
                    torch.as_tensor([signal_size], dtype=torch.int64).detach()
                )
                yield processed_signal, processed_signal_len
    def __len__(self):
        return len(self._signals)

