/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.protobuf.gradle.*

plugins {
    java
    idea
    `maven-publish`
    signing
    id("com.google.protobuf") version "0.9.3"
    id("com.palantir.git-version") version "3.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

val gitVersion: groovy.lang.Closure<String> by extra

val projectVersion: String by lazy {
    val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
    with(versionDetails()) {
        val version = lastTag.removePrefix("v")
        if (commitDistance > 0) {
            val tokens = version.split('.')
            "${tokens[0]}.${tokens[1]}.${tokens[2].toInt() + 1}-SNAPSHOT"
        } else version
    }
}
group = "dev.cerbos"
version = projectVersion

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
        artifact = "com.google.protobuf:protoc:3.22.3"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.54.1"
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
    implementation("com.google.protobuf:protobuf-java:3.22.3")
    implementation("com.google.protobuf:protobuf-java-util:3.22.3")
    implementation("io.grpc:grpc-protobuf:1.54.1")
    implementation("io.grpc:grpc-stub:1.54.1")
    implementation("io.grpc:grpc-netty-shaded:1.54.1")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.60.Final")
    implementation("org.testcontainers:testcontainers:1.18.0")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.testcontainers:junit-jupiter:1.18.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("ch.qos.logback:logback-core:1.4.7")
    testImplementation("ch.qos.logback:logback-classic:1.4.7")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


publishing {
    repositories {
        maven {
            val releasesURL = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsURL = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            name = "OSSRH"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) snapshotsURL else releasesURL
            )
            credentials {
                username = project.findProperty("ossrh.user") as String? ?: System.getenv("OSSRH_USER")
                password = project.findProperty("ossrh.password") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
    publications {
        register<MavenPublication>("ossrh") {
            from(components["java"])
            pom {
                name.set("Cerbos Java SDK")
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
                        email.set("sdk@cerbos.dev")
                    }
                }
                scm {
                    url.set("https://github.com/cerbos/cerbos-sdk-java")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(project.findProperty("ossrh.user") as String? ?: System.getenv("OSSRH_USER"))
            password.set(project.findProperty("ossrh.password") as String? ?: System.getenv("OSSRH_PASSWORD"))
        }
    }
}

signing {
    val signingKeyId = project.findProperty("ossrh.signing.key_id") as String? ?: System.getenv("OSSRH_SIGNING_KEY_ID")
    val signingKey = project.findProperty("ossrh.signing.key") as String? ?: System.getenv("OSSRH_SIGNING_KEY")
    val signingPassword =
        project.findProperty("ossrh.signing.password") as String? ?: System.getenv("OSSRH_SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["ossrh"])
}
