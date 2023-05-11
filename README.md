Cerbos Java SDK
===============

![Maven Central](https://img.shields.io/maven-central/v/dev.cerbos/cerbos-sdk-java?style=for-the-badge&versionPrefix=0.)

Java client library for the [Cerbos](https://github.com/cerbos/cerbos) open source access control solution. This library
includes RPC clients for accessing the Cerbos PDP and test utilities for testing your code locally
using [Testcontainers](https://www.testcontainers.org).

Find out more about Cerbos at https://cerbos.dev and read the documentation at https://docs.cerbos.dev.

Installation
-------------

Artifacts are available from Maven Central.

**Example: Gradle (Kotlin DSL)**

```kotlin
dependencies {
    implementation("dev.cerbos:cerbos-sdk-java")
}

repositories {
    mavenCentral()
}
```

Examples
--------

NOTE: Connecting to Unix domain sockets is not supported by this SDK because the underlying `grpc-java` library does not support them.

### Creating a client without TLS

```java
CerbosBlockingClient client=new CerbosClientBuilder("localhost:3593").withPlaintext().buildBlockingClient();
```

### Check a single principal and resource

```java
CheckResult result=client.check(
    Principal.newInstance("john","employee")
        .withPolicyVersion("20210210")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB")),
    Resource.newInstance("leave_request","xx125")
        .withPolicyVersion("20210210")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB"))
        .withAttribute("owner",stringValue("john")),
    "view:public","approve");

if(result.isAllowed("approve")){ // returns true if `approve` action is allowed
    ...
}
```

### Check a batch

```java
CheckResourcesResult result=client.batch(
    Principal.newInstance("john","employee")
        .withPolicyVersion("20210210")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB"))
    )
    .addResources(
        ResourceAction.newInstance("leave_request","XX125")
            .withPolicyVersion("20210210")
            .withAttributes(
                Map.of(
                    "department", stringValue("marketing"),
                    "geography", stringValue("GB"),
                    "owner", stringValue("john")
                )
            )
            .withActions("view:public","approve","defer"),
        ResourceAction.newInstance("leave_request","XX225")
            .withPolicyVersion("20210210")
            .withAttributes(
                Map.of(
                    "department", stringValue("marketing"),
                    "geography", stringValue("GB"),
                    "owner", stringValue("martha")
                )
            )
            .withActions("view:public","approve"),
        ResourceAction.newInstance("leave_request","XX325")
            .withPolicyVersion("20210210")
            .withAttributes(
                Map.of(
                    "department", stringValue("marketing"),
                    "geography", stringValue("US"),
                    "owner", stringValue("peggy")
                )
            )
            .withActions("view:public","approve")
    )
    .check();

result.find("XX125").map(r->r.isAllowed("view:public")).orElse(false);
```

### Create a query plan

```java
PlanResourcesResult result = client.plan(
    Principal.newInstance("maggie","manager")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB"))
        .withAttribute("team",stringValue("design")),
    Resource.newInstance("leave_request").withPolicyVersion("20210210"),
    "approve"
);

if(result.isAlwaysAllowed()) {
    return true;
} else if(result.isAlwaysDenied()) {
    return false;
} else {
    return executeQuery(result.getCondition());
}
```

### Test with [Testcontainers](https://www.testcontainers.org)

```java
@Container
private static final CerbosContainer cerbosContainer=new CerbosContainer()
    .withClasspathResourceMapping("policies","/policies",BindMode.READ_ONLY)
    .withLogConsumer(new Slf4jLogConsumer(LOG));

@BeforeAll
private void initClient() throws CerbosClientBuilder.InvalidClientConfigurationException{
    String target=cerbosContainer.getTarget();
    this.client=new CerbosClientBuilder(target).withPlaintext().buildBlockingClient();
}
```
