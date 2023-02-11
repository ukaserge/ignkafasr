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

COPY --from=builder /app/.venv ./.venv
# RUN apt update && apt install ffmpeg -y
COPY core ./core

ENTRYPOINT ["/app/.venv/bin/python3.9", "-m", "core.entry"]
