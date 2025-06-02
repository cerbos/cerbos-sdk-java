/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

import com.google.protobuf.gradle.*

plugins {
    java
    idea
    `maven-publish`
    id("com.google.protobuf") version "0.9.5"
    id("com.palantir.git-version") version "3.3.0"
    id("org.jreleaser") version "1.18.0"
    id("com.gradleup.shadow") version "8.3.6"
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
        artifact = "com.google.protobuf:protoc:4.31.1"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.73.0"
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
    implementation("com.google.protobuf:protobuf-java:4.31.1")
    implementation("com.google.protobuf:protobuf-java-util:4.31.1")
    implementation("io.grpc:grpc-protobuf:1.73.0")
    implementation("io.grpc:grpc-stub:1.73.0")
    implementation("io.grpc:grpc-netty-shaded:1.73.0")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.72.Final")
    implementation("org.testcontainers:testcontainers:1.21.1")
    implementation("build.buf.protoc-gen-validate:pgv-java-stub:1.2.1")
    implementation("commons-io:commons-io:2.19.0")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.0")
    testImplementation("org.testcontainers:junit-jupiter:1.21.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.logback:logback-core:1.5.18")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("com.fasterxml.jackson.core:jackson-core:2.19.0")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.shadowJar {
    relocate("com.google.protobuf", "dev.cerbos.shaded.com.google.protobuf")
    relocate("io.grpc", "dev.cerbos.shaded.io.grpc")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
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

configure<org.jreleaser.gradle.plugin.JReleaserExtension> {
    gitRootSearch = true
    project {
        description.set("Java SDK for Cerbos: painless access control for cloud native applications")
        authors.set(listOf("Cerbos Developers"))
        license.set("Apache-2.0")
        version.set(projectVersion)
        links {
            homepage.set("https://github.com/cerbos/cerbos-sdk-java")
            bugTracker.set("https://github.com/cerbos/cerbos-sdk-java/issues")
        }
        inceptionYear.set("2022")
        snapshot {
            fullChangelog.set(true)
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }

    release {
        github {
            enabled.set(false)
        }
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
            nexus2 {
                create("snapshot-deploy") {
                    active.set(org.jreleaser.model.Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                    applyMavenCentralRules.set(true)
                    snapshotSupported.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

