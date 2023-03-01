FROM python:3.9-slim-buster as builder

WORKDIR /app

RUN pip install --upgrade pip && pip install poetry 
COPY pyproject.toml README.md poetry.toml ./
RUN python -m venv .venv
RUN /app/.venv/bin/python3.9 -m pip install --upgrade pip
RUN poetry export -f requirements.txt --without-hashes -o requirements.txt
RUN .venv/bin/pip install -r requirements.txt

##############################
FROM python:3.9-slim-buster as production

WORKDIR /app

RUN apt update && apt-get install -y librdkafka-dev python-dev
RUN apt install -y dumb-init
COPY --from=builder /app/.venv ./.venv
COPY bqproducer ./bqproducer

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/app/.venv/bin/python3.9", "-m", "bqproducer.entry"]
