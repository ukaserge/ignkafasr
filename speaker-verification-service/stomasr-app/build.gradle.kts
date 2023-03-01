import com.google.cloud.tools.jib.api.buildplan.ImageFormat

plugins {
    `java-library`
    `maven-publish`
    id("com.google.cloud.tools.jib") version "3.3.1"
}

repositories {
    mavenCentral()
}

var springVersion = "2.7.6"
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-websocket:$springVersion")
    implementation("org.webjars:stomp-websocket:2.3.4")
    implementation("org.webjars:sockjs-client:1.5.1")
    implementation("org.springframework.kafka:spring-kafka:2.8.11")
    implementation("org.springframework.security:spring-security-messaging:5.7.5")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    implementation("com.google.protobuf:protobuf-java:3.20.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.8.11")
    testImplementation("org.testcontainers:testcontainers:1.17.3")
    testImplementation("org.testcontainers:junit-jupiter:1.17.3")
    testImplementation("org.testcontainers:kafka:1.17.3")
}

group = "limdongjin"
version = "2.9"
description = "stomasr"
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

var jvmOptions = listOf(
    "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED"
)

jib {
    to {
        image = "limdongjin/stomasr"
    }
    from {
        image = "eclipse-temurin:17-jre@sha256:402c656f078bc116a6db1e2e23b08c6f4a78920a2c804ea4c2d3e197f1d6b47c"
    }
    container {
        jvmFlags = jvmOptions
        format = ImageFormat.OCI
    }
}


tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    this.jvmArgs = jvmOptions
    testLogging {
        events("passed")
    }
}
