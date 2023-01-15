# SPBRAIN_VERSION=5.6
# docker build -t spbrain:$SPBRAIN_VERSION . && docker tag spbrain:$SPBRAIN_VERSION gcr.io/limdongjin-kube/spbrain:$SPBRAIN_VERSION && docker push gcr.io/limdongjin-kube/spbrain:$SPBRAIN_VERSION
FROM python:3.9-slim-buster as builder

RUN pip install --upgrade pip && pip install poetry 

WORKDIR /app

COPY pyproject.toml poetry.toml README.md ./
COPY spbrain ./spbrain

RUN poetry config virtualenvs.path .venv
RUN poetry config virtualenvs.prompt sp-prompt
RUN poetry build


FROM python:3.9-slim-buster 

WORKDIR /app

COPY --from=builder /app/.venv ./.venv
COPY --from=builder /app/dist ./dist

RUN .venv/bin/python3.9 -m pip install torch==1.12.1+cpu torchaudio==0.12.1 --extra-index-url https://download.pytorch.org/whl/cpu
RUN .venv/bin/python3.9 -m pip install dist/*.whl

CMD ["/app/.venv/bin/python3.9", "-m", "spbrain.core"]
# CMD ["sleep", "1000"]
