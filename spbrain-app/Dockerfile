# SPBRAIN_VERSION=5.6
# docker build -t spbrain:$SPBRAIN_VERSION . && docker tag spbrain:$SPBRAIN_VERSION gcr.io/limdongjin-kube/spbrain:$SPBRAIN_VERSION && docker push gcr.io/limdongjin-kube/spbrain:$SPBRAIN_VERSION
FROM python:3.9-slim-buster as builder

WORKDIR /app
RUN pip install torch==1.11.0+cpu torchaudio==0.11.0 --extra-index-url https://download.pytorch.org/whl/cpu
RUN apt update && apt install ffmpeg -y
RUN pip install --upgrade pip && pip install poetry 

COPY pyproject.toml poetry.toml poetry.lock README.md ./
RUN poetry export -f requirements.txt --without-hashes | pip install -r /dev/stdin

COPY asrspeech ./asrspeech

CMD ["python", "-m", "asrspeech.core"]
