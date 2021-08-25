/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.protobuf.gradle.*

plugins {
    java
    idea
    `maven-publish`
    id("com.google.protobuf") version "0.8.17"
    id("com.palantir.git-version") version "0.12.3"
}

group = "dev.cerbos.sdk"

val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()

repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

java {
    withJavadocJar()
    withSourcesJar()
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
                id("grpc")
            }
        }
    }
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.17.3")
    implementation("com.google.protobuf:protobuf-java-util:3.17.3")
    implementation("io.grpc:grpc-protobuf:1.40.0")
    implementation("io.grpc:grpc-stub:1.40.0")
    implementation("io.grpc:grpc-netty-shaded:1.40.0")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.40.Final")
    implementation("org.testcontainers:testcontainers:1.16.0")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.testcontainers:junit-jupiter:1.16.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("ch.qos.logback:logback-core:1.2.5")
    testImplementation("ch.qos.logback:logback-classic:1.2.5")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/cerbos/cerbos-sdk-java")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
            pom {
                description.set("Java SDK for Cerbos: painless access control for cloud native applications")
                url.set("https://cerbos.dev")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("cerbosdev")
                        name.set("Cerbos Developers")
                        email.set("help@cerbos.dev")
                    }
                }
                scm {
                    url.set("https://github.com/cerbos/cerbos-sdk-java")
                }
            }
        }
    }
}
