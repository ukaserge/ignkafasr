# IgnKafASR

## About IgnKafAsr

In-memory Speaker Verification and Speech Recognition Project 

using apache ignite, apache kafka, pyannote, speechbrain, whisper, stomp, spring webflux, kubernetes(k8s)

## Demo 

- (closed) [https://kafasr.limdongjin.com](https://kafasr.limdongjin.com)
- [demo video (vimeo)](https://vimeo.com/manage/videos/785495352)

## How to build?

Development: either **docker-compose** or **minikube**
Production: **google kubernetes engine** or other cloud service

### Docker-compose (for Dev)

To complete this, you'll need the following:
- Docker.
- Docker-compose (version3)

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
docker compose up

cd ignkafasr-web
yarn dev
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

docker build -t gcr.io/limdongjin-kube/limdongjin/spbrain -f Dockerfile.prod .
cd $TMPP_HOME

kubectl create -f spbrain/spbrain-sa.yaml
kubectl apply -f spbrain/spbrain-cluster-role.yaml
kubectl create -f spbrain/spbrain-service.yaml
kubectl create -f spbrain/spbrain-deployment.yaml
```

**build ignkafasr-web**
```bash
cd ignkafasr-web
yarn build && gcloud app deploy
```

**Build Complete**

- enter "https://kafasr.limdongjin.com" or other custom domain
