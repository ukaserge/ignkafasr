#
# export MY_KAFKA_HOST=kafka:9092
# export MY_KAFKA_SASL_MECHANISM=PLAIN
# export MY_KAFKA_USER_NAME=admin
# export MY_KAFKA_USER_PASSWORD=hello
#
# sh generate-kafka-info.sh
#

MY_KAFKA_SASL_JAAS_CONFIG="org.apache.kafka.common.security.plain.PlainLoginModule required username='${MY_KAFKA_USER_NAME}' password='${MY_KAFKA_USER_PASSWORD}';"

kubectl create secret generic kafka_info -n limdongjin --from-literal=bootstrap_servers=$MY_KAFKA_HOST --from-literal=security_protocol=$MY_KAFKA_SECURITY_PROTOCOL --from-literal=sasl_mechanism=$MY_KAFKA_SASL_MECHANISM --from-literal=user_name=$MY_KAFKA_USER_NAME --from_literal=user_password=$MY_KAFKA_USER_PASSWORD --from-literal=sasl_jaas_config=$MY_KAFKA_SASL_JAAS_CONFIG
