import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.cloud.tools.jib.api.buildplan.ImageFormat

plugins {
	id("org.springframework.boot") version "3.0.2"
	id("io.spring.dependency-management") version "1.1.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
	`maven-publish`
	id("com.google.cloud.tools.jib") version "3.3.1"
//	id("com.google.protobuf") version "0.9.2"
}

group = "limdongjin"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	gradlePluginPortal()
}
val igniteVersion = "2.14.0"
val springVersion = "3.0.1"
dependencies {
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("org.springframework.boot:spring-boot-starter-webflux:$springVersion")
	implementation("org.springframework.kafka:spring-kafka:$springVersion")
	implementation("io.projectreactor.kafka:reactor-kafka:1.3.15")
	implementation("io.projectreactor:reactor-core:3.5.1")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	implementation("org.apache.ignite:ignite-core:$igniteVersion")
	implementation("org.apache.ignite:ignite-spring:$igniteVersion")
	implementation("org.apache.ignite:ignite-indexing:$igniteVersion")
	implementation("org.apache.ignite:ignite-kubernetes:$igniteVersion")
	implementation("com.google.protobuf:protobuf-java:3.20.3")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
	implementation(platform("com.google.cloud:spring-cloud-gcp-dependencies:4.0.0"))
	implementation("com.google.cloud:spring-cloud-gcp-starter-data-datastore:4.0.0")
//	implementation("org.springframework.cloud:spring-cloud-gcp-starter-data-datastore")
//	implementation("io.grpc:grpc-stub:1.15.1")
//	implementation("io.grpc:grpc-protobuf:1.15.1")
//	protobuf(files("ext/"))

	testImplementation("org.springframework.security:spring-security-test")

	testImplementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
	testImplementation("io.projectreactor:reactor-test:3.5.1")
	testImplementation("org.springframework.kafka:spring-kafka-test")

	testImplementation("org.testcontainers:testcontainers:1.17.3")
	testImplementation("org.testcontainers:junit-jupiter:1.17.3")
	testImplementation("org.testcontainers:kafka:1.17.3")
	testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

var jvmOptions = listOf(
	"--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
	"--add-opens=java.base/java.nio=ALL-UNNAMED",
	"--add-opens=java.base/java.io=ALL-UNNAMED"
)

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

// https://github.com/google/protobuf-gradle-plugin/blob/master/examples/exampleKotlinDslProject/build.gradle.kts
//protobuf {
//	protoc {
//		 The artifact spec for the Protobuf Compiler
//		artifact = "com.google.protobuf:protoc:3.6.1"
//	}
//}

publishing {
	publications.create<MavenPublication>("maven") {
		from(components["java"])
	}
}

// https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin
jib {
	to {
		image = "limdongjin/ignasr"
	}
	from {
		image = "eclipse-temurin:17-jre@sha256:402c656f078bc116a6db1e2e23b08c6f4a78920a2c804ea4c2d3e197f1d6b47c"
	}
	container {
		jvmFlags = jvmOptions
		format = ImageFormat.OCI
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}