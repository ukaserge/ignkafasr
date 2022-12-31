# 0.
./mvnw clean 

# 1. 
./mvnw -DskipTests package

# 2. 
# change version of image
./mvnw -DskipTests com.google.cloud.tools:jib-maven-plugin:build -Dimage=gcr.io/limdongjin-kube/ignasr:v3
