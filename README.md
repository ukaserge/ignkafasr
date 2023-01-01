# IgnKafASR

Apache Ignite + Apache Kafka + ASR + STOMP + Spring + K8S

## Demo 

- (closed) [https://kafasr.limdongjin.com](https://kafasr.limdongjin.com)
- [demo video (vimeo)](https://vimeo.com/manage/videos/785495352)

## Build and deploy

### Google Kubernetes Engine (GKE)

**create namespaces**
```bash
kubectl create ns kafka-cluster
kubectl create ns limdongjin
```

**create kafka cluster**
- strimzi kafka cluster

```bash
helm install my-cluster-op strimzi/strimzi-kafka-operator -n kafka-cluster
kubectl create -f kafka-cluster/kafka/my-cluster.yaml
kubectl create -f kafka-cluster/topic/
kubectl create -f kafka-cluster/user/
```

**create ignite cluster**
- Apache Ignite
- store audio blob.

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

# Change content of "gke/managed-cert.yaml" for your custom domain.
kubectl create -f gke/managed-cert.yaml

# Note that, Wait until the certificate creation is complete.
```

**create ignasr**
- spring webflux + kafka producer + ignite client
- produce "user-pending" event
- upload audio blob to ignite cache

```bash
kubectl create -f ignasr/ignasr-sa.yaml
kubectl apply -f ignasr/ignasr-cluster-role.yaml
kubectl create -f ignasr/ignasr-service-gke.yaml
kubectl create -f ignasr/ignasr-ingress-gke.yaml
kubectl create -f ignasr/ignasr-deployment.yaml
```

**create stomasr**
- Spring stomp + kafka consumer  
- connect browser-stomp
- consume inference result and then send to user.

```bash
kubectl create -f stomasr/stomasr-sa.yaml
kubectl apply -f stomasr/stomasr-cluster-role.yaml
kubectl create -f stomasr/stomasr-service-gke.yaml
kubectl create -f stomasr/stomasr-ingress-gke.yaml
kubectl create -f stomasr/stomasr-deployment.yaml
```

**create spbrain**
- speechbrain + kafka consumer + kafka producer
- consume from 'user-pending' topic
- produce inference result to 'infer-positive' or 'infer-negative'

```bash 
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

- enter "https://ignkasr.limdongjin.com" or other custom domain
