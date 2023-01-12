import com.google.cloud.tools.jib.api.buildplan.ImageFormat.*

plugins {
    `java-library`
    `maven-publish`
    id("com.google.cloud.tools.jib") version "3.3.1"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

val igniteVersion = "2.14.0"
val springVersion = "3.0.1"
dependencies {
    api("org.springframework.boot:spring-boot-starter-webflux:$springVersion")
    api("org.springframework.kafka:spring-kafka:$springVersion")
    api("io.projectreactor.kafka:reactor-kafka:1.3.15")
    api("io.projectreactor:reactor-core:3.5.1")

    api("org.apache.ignite:ignite-core:$igniteVersion")
    api("org.apache.ignite:ignite-spring:$igniteVersion")
    api("org.apache.ignite:ignite-indexing:$igniteVersion")
    api("org.apache.ignite:ignite-kubernetes:$igniteVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
    testImplementation("io.projectreactor:reactor-test:3.5.1")

    testImplementation("org.testcontainers:testcontainers:1.17.3")
    testImplementation("org.testcontainers:junit-jupiter:1.17.3")
    testImplementation("org.testcontainers:kafka:1.17.3")
}

group = "limdongjin"
version = "0.0.4-SNAPSHOT"
description = "ignasr"


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
        image = "limdongjin/ignasr"
    }
    from {
        image = "eclipse-temurin:17-jre@sha256:402c656f078bc116a6db1e2e23b08c6f4a78920a2c804ea4c2d3e197f1d6b47c"
    }
    container {
        jvmFlags = jvmOptions
        format = OCI
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
