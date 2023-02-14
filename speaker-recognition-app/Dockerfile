FROM python:3.9-slim-buster as builder
WORKDIR /app

RUN pip install --upgrade pip && pip install poetry 

COPY pyproject.toml poetry.toml README.md ./

RUN python -m venv .venv

RUN /app/.venv/bin/python3.9 -m pip install --upgrade pip
RUN poetry export -f requirements.txt --without-hashes -o requirements.txt
RUN .venv/bin/pip install -r requirements.txt

FROM python:3.9-slim-buster as production
WORKDIR /app

# RUN apt update && apt-get install -y sox libsndfile1 ffmpeg
RUN apt update && apt-get install -y libsndfile1

COPY --from=builder /app/.venv ./.venv

# RUN /app/.venv/bin/python3.9 -m pip install Cython
RUN /app/.venv/bin/python3.9 -m pip install torch==1.12.0+cpu torchaudio==0.12.0 --extra-index-url https://download.pytorch.org/whl/cpu
# RUN /app/.venv/bin/python3.9 -m pip install nemo_toolkit[asr]

COPY core ./core

# CMD ["sleep", "10000"]

ENTRYPOINT ["/app/.venv/bin/python3.9", "-m", "core.entry"]
