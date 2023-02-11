# for development
## docker build -t limdongjin/spbrain .
# for production
## docket build -t gcr.io/limdongjin-kube/spbrain && docker push gcr.io/limdongjin-kube/spbrain

FROM python:3.9-slim-buster as production

RUN apt update && apt-get update && apt install ffmpeg -y

FROM python:3.9-slim-buster as builder
WORKDIR /app

RUN pip install --upgrade pip && pip install poetry 

COPY pyproject.toml poetry.toml README.md ./

RUN python -m venv .venv
# RUN pip install torch==1.12.0+cpu torchaudio==0.12.0 --extra-index-url https://download.pytorch.org/whl/cpu

RUN /app/.venv/bin/python3.9 -m pip install --upgrade pip
RUN poetry export -f requirements.txt --without-hashes -o requirements.txt
RUN .venv/bin/pip install -r requirements.txt

FROM production as final

WORKDIR /app

COPY --from=builder /app/.venv ./.venv
COPY core ./core

ENTRYPOINT ["/app/.venv/bin/python3.9", "-m", "core.entry"]
