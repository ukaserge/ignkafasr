FROM python:3.9-slim-buster as production

RUN apt update && apt-get update && apt install ffmpeg -y
RUN apt-get install -y sox libsndfile1 librdkafka-dev python-dev

FROM python:3.9-slim-buster as builder
WORKDIR /app

RUN pip install --upgrade pip && pip install poetry 

COPY pyproject.toml poetry.toml README.md ./

RUN python -m venv .venv

RUN /app/.venv/bin/python3.9 -m pip install --upgrade pip
RUN poetry export -f requirements.txt --without-hashes -o requirements.txt
RUN .venv/bin/pip install -r requirements.txt

FROM production as final

RUN apt install -y dumb-init 

WORKDIR /app

COPY --from=builder /app/.venv ./.venv
COPY dlytaudio ./dlytaudio

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/app/.venv/bin/python3.9", "-m", "dlytaudio.entry"]
