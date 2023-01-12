# IgnASR

## About ignasr
- spring webflux + kafka producer + ignite thin client
- produce "user-pending" event
- upload audio blob to ignite cache
- see also `src/test/java/limdongjin/ignasr/*` 

## Build and Test

- you can test or build using this script:
```bash
./mvnw clean

# Unit Test
./mvnw "-Dtest=MultiPartUtil*,SpeechUploadHandlerTest.java" test

# Integration Test (requirements: docker engine running)
./mvnw "-Dtest=SpeechUploadHandlerIntegrationTest.java" test

# BUILD to local registry
./mvnw clean
./mvnw compile -DskipTests com.google.cloud.tools:jib-maven-plugin:dockerBuild -Dimage=limdongjin/ignasr

# Build to Google Container Registry
# must change image repository, name, version
./mvnw clean
./mvnw compile -DskipTests com.google.cloud.tools:jib-maven-plugin:build -Dimage=gcr.io/limdongjin-kube/ignasr:v3

```
 
