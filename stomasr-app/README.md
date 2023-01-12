# StomASR

## About stomasr
- spring stomp + kafka consumer
- establish connection between client(browser) and stomp-server
- consume inference result from kafka topic and send result to client.

## Build and Test

- you can test or build using this script:
```bash
./mvnw clean

# Unit Test
./mvnw "-Dtest=SuccListener*,SuccService*" test

# Integration Test (requirements: docker engine running)
./mvnw "-Dtest=SuccIntegration*" test

# BUILD
# must change image repository, name, version
./mvnw -DskipTests package

./mvnw -DskipTests com.google.cloud.tools:jib-maven-plugin:build -Dimage=gcr.io/limdongjin-kube/stomasr:v3
```

