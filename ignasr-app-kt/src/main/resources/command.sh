# https://www.baeldung.com/linux/public-key-from-private-key

openssl genrsa -out private_key.pem 2048

openssl pkcs8 -topk8 -inform PEM -in private_key.pem -out token_key.pem -nocrypt

openssl pkey -in private.pem -pubout -out public.pem