# docker build -t spbrain:5.5 . && docker tag spbrain:5.5 gcr.io/limdongjin-kube/spbrain:5.5 && docker push gcr.io/limdongjin-kube/spbrain:5.5
FROM python:3.8-buster
RUN apt-get update && \
    apt-get install --no-install-suggests --no-install-recommends --yes python3-venv gcc libpython3-dev
COPY requirements.txt /requirements.txt
RUN pip install -r /requirements.txt
COPY . /app
WORKDIR /app

ENTRYPOINT ["python", "my_main2.py"]
