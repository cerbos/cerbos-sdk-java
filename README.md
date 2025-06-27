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
    implementation("dev.cerbos:cerbos-sdk-java:0.+")
    implementation("io.grpc:grpc-core:1.+")
}

repositories {
    mavenCentral()
}
```

Examples
--------

> [!NOTE]
> Connecting to Unix domain sockets using this SDK is only supported on Linux, which is a limitation inherited from the underlying [`grpc-java`](https://github.com/grpc/grpc-java) library.

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

### Accessing the Admin API

```java
// Username and password can be specified using CERBOS_USER and CERBOS_PASSWORD environment variables as well
CerbosBlockingAdminClient  adminClient = new CerbosClientBuilder(target).withPlaintext().buildBlockingAdminClient("username", "password");

adminClient.addOrUpdatePolicy().with(new FileReader(fileObjectContainingPolicyJSON)).addOrUpdate();
```

See `CerbosBlockingAdminClientTest` test class for more examples of Admin API usage including how to convert YAML policies to the JSON format required by the  API.

## Connecting to Cerbos Hub stores

Log in to Cerbos Hub and generate a client credential for the store you wish to connect. Create two environment variables named `CERBOS_HUB_CLIENT_ID` and `CERBOS_HUB_CLIENT_SECRET` to hold the credentials.   

```java
CerbosHubStoreClient client = CerbosHubClientBuilder.fromEnv().build().storeClient();
try {
    Store.ReplaceFilesResponse resp = client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip("path/to/dir")));
    System.out.println(resp.getNewStoreVersion());
} catch (StoreException se) {
    ...
}
```

It's possible to obtain more information about errors by catching the specific exception class (e.g. `dev.cerbos.sdk.hub.exceptions.ValidationFailureException`) or by catching `dev.cerbos.sdk.hub.exceptions.StoreException`, calling `getReason()` to determine the reason for the exception and then casting the exception to the appropriate exception subclass.

```java
// Catching a specific exception
try {
    Store.ReplaceFilesResponse resp = client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip("path/to/dir")));
    System.out.println(resp.getNewStoreVersion());
        } catch (NoUsableFilesException nufe) {
        nufe.getIgnoredFiles().stream().forEach(System.out::println);
} catch (StoreException se) {
        // Catch-all 
}

// Catching StoreException and casting
try {
    Store.ReplaceFilesResponse resp = client.replaceFiles(Store.newReplaceFilesRequest(storeID, "Reset store", Utils.createZip("path/to/dir")));
    System.out.println(resp.getNewStoreVersion());
        } catch (StoreException se) {
        if (se.getReason() == StoreException.Reason.NO_USABLE_FILES) {
NoUsableFilesException exception = (NoUsableFilesException) se;
        exception.getIgnoredFiles().stream().forEach(System.out::println);
    }
}
```

## Common issues

`java.lang.IllegalArgumentException: cannot find a NameResolver for ...`:
   The gRPC library relies on Java SPI to register name resolvers and client-side load balancing strategies for clients. The defaults are defined in the `io.grpc:grpc-core` library. Some packaging methods could overwrite or strip out the `META-INF/services` directory, which would cause the above exception on Cerbos client initialisation. If that's the case, eithertry to recreate the [default service bindings](https://github.com/grpc/grpc-java/tree/master/core/src/main/resources/META-INF/services) in your own jar OR explicitly register the services as follows:

   ```java
   import io.grpc.LoadBalancerRegistry;
   import io.grpc.NameResolverRegistry;

   public class Cerbos {
     public static void main(String[] args) throws CerbosClientBuilder.InvalidClientConfigurationException {
       LoadBalancerRegistry.getDefaultRegistry().register(new io.grpc.internal.PickFirstLoadBalancerProvider());
       NameResolverRegistry.getDefaultRegistry().register(new io.grpc.internal.DnsNameResolverProvider());
       CerbosBlockingClient client = new CerbosClientBuilder("dns:///cerbos.my-ns.svc.cluster.local:3593").withInsecure().buildBlockingClient();
       ...
     }
   }
   ```
