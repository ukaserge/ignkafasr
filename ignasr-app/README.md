# IgnAsr

## Build 

- you can test or build using this script:
```bash
./gradlew clean

# BUILD to local registry
./gradlew jibDockerBuild --image=limdongjin/ignasr

# Build to Google Container Registry
# must change image repository, name, version
./gradlew jib --image=gcr.io/limdongjin-kube/ignasr
```

