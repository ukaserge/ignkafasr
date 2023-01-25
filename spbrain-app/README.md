# spbrain

- speechbrain + whisper + kafka
- consume request from 'user-pending' topic
- produce inference result to 'infer' topic

## Build

- you can build using this script:
```bash
# development
docker build -t limdongjin/spbrain .

# production
docker build -t gcr.io/limdongjin-kube/spbrain -f Dockerfile.prod .
```
