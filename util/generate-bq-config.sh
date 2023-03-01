kubectl create configmap bq-config --from-file ../dev/secrets/bq-config.json --dry-run=client -o yaml > ../bqproducer/bq-config.yaml
