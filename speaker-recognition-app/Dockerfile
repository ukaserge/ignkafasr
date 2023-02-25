FROM python:3.9-slim-buster as builder

WORKDIR /app

RUN apt update && apt install -y make gcc g++ wget git
RUN git clone https://github.com/ggerganov/whisper.cpp

WORKDIR /app/whisper.cpp
RUN make tiny
RUN gcc -O3 -std=c11   -pthread -mavx -mavx2 -mfma -mf16c -fPIC -c ggml.c
RUN g++ -O3 -std=c++11 -pthread --shared -fPIC -static-libstdc++ whisper.cpp ggml.o -o libwhisper.so

WORKDIR /app
RUN pip install --upgrade pip && pip install poetry 
COPY pyproject.toml poetry.toml README.md ./
RUN python -m venv .venv
RUN /app/.venv/bin/python3.9 -m pip install --upgrade pip
RUN /app/.venv/bin/python3.9 -m pip install Cython
RUN poetry export -f requirements.txt --without-hashes -o requirements.txt
RUN .venv/bin/pip install -r requirements.txt

##############################
FROM python:3.9-slim-buster as production

WORKDIR /app

RUN apt update && apt-get install -y sox libsndfile1 ffmpeg
RUN apt install -y gcc g++
RUN apt-get install -y librdkafka-dev python-dev
RUN apt install -y dumb-init
COPY --from=builder /app/.venv ./.venv
COPY core ./core

WORKDIR /app/whisper.cpp
COPY --from=builder /app/whisper.cpp/libwhisper.so ./libwhisper.so
COPY --from=builder /app/whisper.cpp/ggml.o ./ggml.o
COPY --from=builder /app/whisper.cpp/models/ggml-tiny.bin ./models/ggml-tiny.bin

WORKDIR /app

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/app/.venv/bin/python3.9", "-m", "core.entry"]
