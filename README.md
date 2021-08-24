Cerbos Java SDK
===============

Java client library for the [Cerbos](https://github.com/cerbos/cerbos) open source access control solution. This library
includes RPC clients for accessing the Cerbos PDP and test utilities for testing your code locally
using [Testcontainers](https://www.testcontainers.org).

Find out more about Cerbos at https://cerbos.dev and read the documentation at https://docs.cerbos.dev.

Installation
-------------

- Configure https://maven.pkg.github.com/cerbos/cerbos-sdk-java as Maven repository in your repository manager or the
  build settings.
- Add `dev.cerbos.sdk:cerbos-sdk-java:0.1.0` as a dependency.

**Example: Gradle (Kotlin DSL)**

```kotlin
dependencies {
    implementation("dev.cerbos.sdk:cerbos-sdk-java:0.1.0")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/cerbos/cerbos-sdk-java")
    }
}
```

Examples
--------

### Creating a client without TLS

```java
CerbosBlockingClient client=new CerbosClientBuilder("localhost:3593").withPlaintext().buildBlockingClient();
```

### Check a single principal and resource

```java
CheckResult result=
        client.check(
        Principal.newInstance("john","employee")
        .withPolicyVersion("20210210")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB")),
        Resource.newInstance("leave_request","xx125")
        .withPolicyVersion("20210210")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB"))
        .withAttribute("owner",stringValue("john")),
        "view:public",
        "approve");

        result.isAllowed("approve"); // returns true if `approve` action is allowed
```

### Check a batch

```java
CheckResourceSetResult result=
        client.withPrincipal(
        Principal.newInstance("john","employee")
        .withPolicyVersion("20210210")
        .withAttribute("department",stringValue("marketing"))
        .withAttribute("geography",stringValue("GB"))
        )
        .withResourceKind("leave_request","20210210")
        .withActions("view:public","approve")
        .withResource("XX125",Map.of(
        "department",stringValue("marketing"),
        "geography",stringValue("GB"),
        "owner",stringValue("john"))
        )
        .withResource("XX225",Map.of(
        "department",stringValue("marketing"),
        "geography",stringValue("GB"),
        "owner",stringValue("martha"))
        )
        .withResource("XX325",Map.of(
        "department",stringValue("marketing"),
        "geography",stringValue("US"),
        "owner",stringValue("peggy"))
        )
        .check();

        result.isAllowed("XX125","view:public"); // returns true if view:public is allowed on resource XX125
```

### Test with [Testcontainers](https://www.testcontainers.org)

```java
@Container
private static final CerbosContainer cerbosContainer=
        new CerbosContainer("0.5.0")
        .withClasspathResourceMapping("policies","/policies",BindMode.READ_ONLY)
        .withLogConsumer(new Slf4jLogConsumer(LOG));

@BeforeAll
private void initClient()throws CerbosClientBuilder.InvalidClientConfigurationException{
        String target=cerbosContainer.getTarget();
        this.client=new CerbosClientBuilder(target).withPlaintext().buildBlockingClient();
        }
```



