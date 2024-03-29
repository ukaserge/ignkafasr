# IgnKafASR

## About IgnKafAsr

Real-Time In-memory Speaker Verification and Speech Recognition Project 

using apache ignite, apache kafka, speechbrain, whisper, stomp, spring webflux, kubernetes(k8s)

## Demo 

- (closed) [https://kafasr.limdongjin.com](https://kafasr.limdongjin.com)
- [demo video (ver1) ](https://vimeo.com/manage/videos/785495352)
- [demo video (ver2)](https://www.youtube.com/watch?v=VZdIU6MMds4)
- [demo video1 (ver3)](https://www.youtube.com/watch?v=eE-AVDOvirI&feature=youtu.be)
- [demo video2 (ver3)](https://www.youtube.com/watch?v=KfEcL1IQJ4w)
- [demo video (ver4)](https://www.youtube.com/watch?v=u1kSrlhpGNo)

## How to build?

- Development: either **docker-compose** or **minikube**
- Production: **google kubernetes engine** or other

### Docker-compose (for Dev)

To complete this, you'll need the following:
- Docker
- Docker-compose v3

```bash
git clone https://github.com/limdongjin/ignkafasr
cd ignkafasr
TMPP_HOME=$(pwd)

cd $TMPP_HOME/ignasr-app
./gradlew jibDockerBuild --image=limdongjin/ignasr

cd $TMPP_HOME/stomasr-app
./gradlew jibDockerBuild --image=limdongjin/stomasr

cd $TMPP_HOME/spbrain-app
docker build -t limdongjin/spbrain .

cd $TMPP_HOME/dev
docker compose up -d

cd $TMPP_HOME/ignkafasr-web
yarn dev

# speaker verification url: localhost:3000/main 
# speaker registration url: localhost:3000/upload
```

### Google Kubernetes Engine (for Prod)

To complete this, you'll need the following:
- google kubernetes engine cluster
- gcloud, kubectl
  - [Install kubectl and configure gke cluster access](https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl)
- your custom domain (for CORS issue)
- edit some k8s yaml codes in this repo (domain name, container registry repo name, ssl cert config, ...)

**create namespaces**
```bash
kubectl create ns kafka-cluster
kubectl create ns limdongjin
```

**create kafka cluster**
```bash
helm repo add strimzi https://strimzi.io/charts
helm install my-cluster-op strimzi/strimzi-kafka-operator -n kafka-cluster

kubectl create -f kafka-cluster/kafka/my-cluster.yaml
kubectl create -f kafka-cluster/topic/
kubectl create -f kafka-cluster/user/
```

**create ignite cluster**
```bash
kubectl create -f ignite/ignite-sa.yaml
kubectl apply -f ignite/ignite-cluster-role.yaml
kubectl create -f ignite/ignite-configmap.yaml
kubectl create -f ignite/ignite-service.yaml
kubectl create -f ignite/ignite-deployment.yaml

kubectl get pods -n limdongjin
kubectl exec -n limdongjin -it <your-ignite-cluster-pod0-name> -- /opt/ignite/apache-ignite/bin/control.sh --set-state ACTIVE --yes

kubectl exec -n limdongjin -it <your-ignite-cluster-pod1-name> -- /opt/ignite/apache-ignite/bin/control.sh --set-state ACTIVE --yes

kubectl exec -n limdongjin -it <your-ignite-cluster-pod2-name> -- /opt/ignite/apache-ignite/bin/control.sh --set-state ACTIVE --yes
```

**create secrets for SCRAM-SHA-512 Auth**
```bash
kubectl create secret generic ignasr -n limdongjin --from-literal=sasl.jaas.config="$(kubectl get secret ignasr -n kafka-cluster -o jsonpath="{.data.sasl\.jaas\.config}" | base64 -d)"

kubectl create secret generic stomasr -n limdongjin --from-literal=sasl.jaas.config="$(kubectl get secret stomasr -n kafka-cluster -o jsonpath="{.data.sasl\.jaas\.config}" | base64 -d)"

kubectl create secret generic spbrain -n limdongjin --from-literal=password="$(kubectl get secret spbrain -n kafka-cluster -o jsonpath="{.data.password}" | base64 -d)"

kubectl create secret generic spbrain-t -n limdongjin --from-literal=password="$(kubectl get secret spbrain-t -n kafka-cluster -o jsonpath="{.data.password}" | base64 -d)"
```

**create static ip and managed-certificate (GKE)**
```bash
gcloud compute addresses create ignasr-static-ip --global
gcloud compute addresses create stomasr-static-ip --global

kubectl create -f gke/managed-cert.yaml
```

**create ignasr**
```bash
TMPP_HOME=$(pwd)
cd $TMPP_HOME/ignasr-app

./gradlew jib --image=gcr.io/limdongjin-kube/ignasr
cd $TMPP_HOME

kubectl create -f ignasr/ignasr-sa.yaml
kubectl apply -f ignasr/ignasr-cluster-role.yaml
kubectl create -f ignasr/ignasr-service-gke.yaml
kubectl create -f ignasr/ignasr-ingress-gke.yaml
kubectl create -f ignasr/ignasr-deployment.yaml
```

**create stomasr**
```bash
TMPP_HOME=$(pwd)
cd $TMPP_HOME/stomasr-app

./gradlew jib --image=gcr.io/limdongjin-kube/stomasr
cd $TMPP_HOME

kubectl create -f stomasr/stomasr-sa.yaml
kubectl apply -f stomasr/stomasr-cluster-role.yaml
kubectl create -f stomasr/stomasr-service-gke.yaml
kubectl create -f stomasr/stomasr-ingress-gke.yaml
kubectl create -f stomasr/stomasr-deployment.yaml
```

**create spbrain**
```bash 
TMPP_HOME=$(pwd)
cd $TMPP_HOME/spbrain-app

docker build -t gcr.io/limdongjin-kube/spbrain .
docker build -t gcr.io/limdongjin-kube/spbrain-t -f Dockerfile.transcribe .
docker push gcr.io/limdongjin-kube/spbrain
docker push gcr.io/limdongjin-kube/spbrain-t
cd $TMPP_HOME

kubectl create -f spbrain/spbrain-sa.yaml
kubectl apply -f spbrain/spbrain-cluster-role.yaml
kubectl create -f spbrain/spbrain-service.yaml
kubectl create -f spbrain/spbrain-deployment.yaml
kubectl create -f spbrain/spbrain-t-deployment.yaml
```

**build ignkafasr-front**
```bash
cd ignkafasr-front
yarn build && gcloud app deploy
```

**Build Complete**

- enter "https://kafasr.limdongjin.com" or other custom domain
