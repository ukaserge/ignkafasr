FROM pytorch/pytorch:1.5-cuda10.1-cudnn7-runtime
RUN apt-get update && \
    apt-get install --no-install-suggests --no-install-recommends --yes python3-venv gcc libpython3-dev
COPY requirements.txt /requirements.txt
RUN pip install -r /requirements.txt
COPY . /app
WORKDIR /app

ENTRYPOINT ["python", "my_main.py"]
