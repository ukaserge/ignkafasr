# IgnASR

## About ignasr
- spring webflux + kafka producer + ignite thin client
- produce "user-pending" event
- upload audio blob to ignite cache
- see also `src/test/java/limdongjin/ignasr/*` 

## Build and Test

- you can test or build using this script:
```bash
./gradlew clean

# Unit Test
./gradlew test --tests "MultiPart*" --tests "SpeechUploadHandlerTest*"

# Integration Test (requirements: docker engine running)
 ./gradlew test --tests "*IntegrationTest*"
 
# BUILD to local registry
./gradlew clean
./gradlew jibDockerBuild

# Build to Google Container Registry
# must change image repository, name
./gradlew jib --image=gcr.io/limdongjin-kube/ignasr

```
 
