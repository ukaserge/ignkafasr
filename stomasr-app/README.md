# StomASR

## About stomasr
- spring stomp + kafka consumer
- establish connection between client(browser) and stomp-server
- consume inference result from kafka topic and send result to client.

## Build and Test

- you can test or build using this script:
```bash
./gradlew clean

# Unit Test
./gradlew test --tests "SuccListener*" --tests "SuccService*"

# Integration Test (requirements: docker engine running)
./gradlew test --tests "SuccIntegration*"

# BUILD to local registry
./gradlew jibDockerBuild

# Build to Google Container Registry
# must change image repository, name, version
./gradlew jib --image=gcr.io/limdongjin-kube/stomasr
```

