import com.google.protobuf.gradle.*

plugins {
    java
    id("com.google.protobuf") version "0.8.17"
}

group = "dev.cerbos.sdk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")

    /*
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/cerbos/client-protos")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
     */
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.17.3"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.40.0"
        }
    }

    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.17.3")
    implementation("io.grpc:grpc-protobuf:1.40.0")
    implementation("io.grpc:grpc-stub:1.40.0")
    implementation("io.grpc:grpc-netty-shaded:1.40.0")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.40.Final")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.testcontainers:testcontainers:1.16.0")
    testImplementation("org.testcontainers:junit-jupiter:1.16.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("ch.qos.logback:logback-core:1.2.5")
    testImplementation("ch.qos.logback:logback-classic:1.2.5")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}