# Requirements: cerbos-sdk-java

Reverse engineered from the code at `6ba1f6844beb` (2026-09-28). Generated from `requirements.json`; edit that file and re-render instead of editing this one.

In scope: `src/main/java/dev/cerbos/sdk/**`
Out of scope: `src/main/proto/**`, `generated dev/cerbos/api/** code`, `.github/**`, `src/test/java/dev/cerbos/sdk/PlaygroundIT.java`, `src/main/java/dev/cerbos/sdk/hub/** (Cerbos Hub store client)`, `src/main/resources/service_config.json (Hub gRPC service config)`, `src/test/java/dev/cerbos/sdk/hub/**`

## Summary

| Feature | implemented-tested | implemented-untested | tested-only | documented-only | unevidenced | retired |
|---|---|---|---|---|---|---|
| PDP and admin client configuration | 3 | 11 | 0 | 0 | 0 | 0 |
| Authorisation checks | 8 | 5 | 0 | 0 | 0 | 0 |
| Query planning | 6 | 0 | 0 | 0 | 0 | 0 |
| Admin API | 10 | 2 | 0 | 0 | 0 | 0 |
| Testcontainers support | 2 | 0 | 0 | 0 | 0 | 0 |
| **Total** | 29 | 18 | 0 | 0 | 0 | 0 |

## PDP and admin client configuration (`CLIENT-CONFIG`)

Building PDP and admin clients: transport security, timeouts, credentials, headers, audit annotations and RPC error reporting.

| ID | Requirement | Kind | Risk | Status | Confidence |
|---|---|---|---|---|---|
| REQ-CLIENT-CONFIG-001 | Reject a missing server target | validation | low | implemented-untested | medium |
| REQ-CLIENT-CONFIG-002 | TLS by default, plaintext on request | configuration | high | implemented-untested | medium |
| REQ-CLIENT-CONFIG-003 | Insecure mode trusts any server certificate | configuration | high | implemented-untested | medium |
| REQ-CLIENT-CONFIG-004 | Custom CA certificate | configuration | high | implemented-untested | medium |
| REQ-CLIENT-CONFIG-005 | Mutual TLS client certificate | configuration | high | implemented-untested | medium |
| REQ-CLIENT-CONFIG-006 | Authority override | configuration | low | implemented-untested | medium |
| REQ-CLIENT-CONFIG-007 | Caller-supplied gRPC interceptors | integration | low | implemented-untested | medium |
| REQ-CLIENT-CONFIG-008 | Per-call deadline | non-functional | medium | implemented-untested | medium |
| REQ-CLIENT-CONFIG-009 | Playground instance header | integration | low | implemented-untested | medium |
| REQ-CLIENT-CONFIG-010 | Admin client uses Basic credentials | authorisation | high | implemented-tested | high |
| REQ-CLIENT-CONFIG-011 | Admin credentials from environment | configuration | medium | implemented-untested | medium |
| REQ-CLIENT-CONFIG-012 | Custom request headers | integration | low | implemented-untested | medium |
| REQ-CLIENT-CONFIG-013 | Audit annotations on requests | integration | medium | implemented-tested | medium |
| REQ-CLIENT-CONFIG-014 | RPC failures raised as CerbosException | error-handling | high | implemented-tested | high |

### REQ-CLIENT-CONFIG-001 Reject a missing server target

When the target address is null or blank, building a PDP or admin client fails with InvalidClientConfigurationException ("Invalid target [...]") and no connection is opened.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:31-33` (isEmptyString); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:80-83` (CerbosClientBuilder.buildChannel)
- Conditions: target in {null, blank, non-blank}

| target | Expected | Tests |
|---|---|---|
| null | InvalidClientConfigurationException | 0 |
| blank | InvalidClientConfigurationException | 0 |
| non-blank | client built | 0 |

### REQ-CLIENT-CONFIG-002 TLS by default, plaintext on request

The client connects over TLS and verifies the server against the JVM's default trust store, unless the caller asks for plaintext, in which case it connects with no transport security.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:35-38` (withPlaintext); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:85-111` (CerbosClientBuilder.buildChannel)
- Conditions: plaintext in {on, off}

| plaintext | Expected | Tests |
|---|---|---|
| on | unencrypted gRPC channel | 0 |
| off | TLS channel with default trust | 0 |

Every integration test builds its client withPlaintext(), so the TLS branch never runs in the suite. The fixture certificates in src/test/resources/certificates are not used by any test.

### REQ-CLIENT-CONFIG-003 Insecure mode trusts any server certificate

When the caller enables insecure mode on a TLS connection, the client accepts any server certificate without verifying it. Insecure mode has no effect when plaintext is also enabled.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:40-43` (withInsecure); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:89-92`
- Conditions: plaintext in {on, off}; insecure in {on, off}; ca_certificate in {set, unset}

| plaintext | insecure | ca_certificate | Expected | Tests |
|---|---|---|---|---|
| off | on | unset | any server certificate accepted | 0 |
| on | on | unset | plaintext; insecure ignored | 0 |
| off | on | set | CA trust manager is set after the insecure one, so the CA certificate is what gets used | 0 |

With both insecure mode and a CA certificate, the code calls trustManager() twice, and the CA certificate call comes second. On gRPC's TlsChannelCredentials.Builder the later call replaces the earlier one.
- Open question: When both withInsecure() and withCaCertificate() are set, should insecure mode win? The code currently gives the CA certificate precedence.

### REQ-CLIENT-CONFIG-004 Custom CA certificate

When the caller supplies a CA certificate, the client trusts servers signed by that CA. If the certificate cannot be loaded, building the client fails with InvalidClientConfigurationException ("Failed to set CA trust root").

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:50-53` (withCaCertificate); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:94-100`
- Conditions: ca_certificate in {unset, valid, unreadable}

| ca_certificate | Expected | Tests |
|---|---|---|
| valid | server verified against the supplied CA | 0 |
| unreadable | InvalidClientConfigurationException: Failed to set CA trust root | 0 |

### REQ-CLIENT-CONFIG-005 Mutual TLS client certificate

When the caller supplies both a client certificate and a private key, the client presents them to the server. If only one of the two is supplied, both are silently ignored. If loading fails, building fails with InvalidClientConfigurationException ("Failed to set TLS credentials").

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:55-63`; `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:102-108`
- Conditions: tls_certificate in {set, unset}; tls_key in {set, unset}; load_result in {ok, error}

| tls_certificate | tls_key | load_result | Expected | Tests |
|---|---|---|---|---|
| set | set | ok | client certificate presented | 0 |
| set | unset |  | no client certificate, no error | 0 |
| unset | set |  | no client certificate, no error | 0 |
| set | set | error | InvalidClientConfigurationException: Failed to set TLS credentials | 0 |
- Open question: Should supplying a certificate without a key (or the reverse) be a configuration error instead of being silently ignored?

### REQ-CLIENT-CONFIG-006 Authority override

When the caller sets a non-blank authority, the client uses it as the TLS/HTTP2 authority instead of the target host name.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:45-48` (withAuthority); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:113-115`
- Conditions: authority in {null, blank, non-blank}

| authority | Expected | Tests |
|---|---|---|
| non-blank | authority overridden | 0 |
| blank | target host used | 0 |

### REQ-CLIENT-CONFIG-007 Caller-supplied gRPC interceptors

The client runs any gRPC client interceptors the caller registers on every call it makes.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:75-78` (withClientInterceptors); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:117-119`

### REQ-CLIENT-CONFIG-008 Per-call deadline

Every PDP and admin call must complete within the configured timeout, 1 second by default, or it fails with DEADLINE_EXCEEDED. The caller can change the timeout when building the client.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:24-24`; `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:65-68` (withTimeout); `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:63-66` (withClient); `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:42-45` (withClient)
- Conditions: timeout in {default (1000 ms), custom}

| timeout | Expected | Tests |
|---|---|---|
| default (1000 ms) | deadline 1000 ms per call | 0 |
| custom | deadline as configured | 0 |

Integration tests run with the default timeout but never check it.

### REQ-CLIENT-CONFIG-009 Playground instance header

When the caller sets a non-blank playground instance id, every PDP call carries it in a playground-instance header. Admin clients never send this header.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:70-73` (withPlaygroundInstance); `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:124-130` (buildBlockingClient); `src/main/java/dev/cerbos/sdk/PlaygroundInstanceCredentials.java:13-31`; `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:40-52`
- Conditions: playground_instance in {null, blank, non-blank}

| playground_instance | Expected | Tests |
|---|---|---|
| non-blank | header playground-instance sent | 0 |
| blank | no header | 0 |

PlaygroundIT exercises this, but it is a manual main() with no assertions, so it doesn't count as a test.

### REQ-CLIENT-CONFIG-010 Admin client uses Basic credentials

An admin client sends the given username and password as an HTTP Basic authorization header on every admin call. If either value is null, building the admin client fails with InvalidClientConfigurationException ("username and password must not be null"). Empty strings are accepted.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:138-145` (buildBlockingAdminClient); `src/main/java/dev/cerbos/sdk/AdminApiCredentials.java:15-34`; `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:29-34`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithoutFilter`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::getPolicy`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listSchemas`
- Conditions: username in {null, empty, non-empty}; password in {null, empty, non-empty}

| username | password | Expected | Tests |
|---|---|---|---|
| non-empty | non-empty | admin calls authenticated | 1 |
| null | non-empty | InvalidClientConfigurationException | 0 |
| non-empty | null | InvalidClientConfigurationException | 0 |
| empty | empty | client built; server decides | 0 |

The admin tests authenticate as cerbos/cerbosAdmin. Their passing shows the header is accepted, but no test covers rejection.

### REQ-CLIENT-CONFIG-011 Admin credentials from environment

When no credentials are passed, the admin client reads CERBOS_USERNAME and CERBOS_PASSWORD from the environment. If either variable is missing, building fails with InvalidClientConfigurationException.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:132-136` (buildBlockingAdminClient())
- Conditions: CERBOS_USERNAME in {set, unset}; CERBOS_PASSWORD in {set, unset}

| CERBOS_USERNAME | CERBOS_PASSWORD | Expected | Tests |
|---|---|---|---|
| set | set | client built | 0 |
| unset | set | InvalidClientConfigurationException | 0 |

### REQ-CLIENT-CONFIG-012 Custom request headers

The caller can derive a PDP or admin client that sends extra headers, given as a map or as gRPC Metadata, on every call. The original client is left unchanged. Passing null Metadata removes the extra headers.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:84-98` (withHeaders); `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:53-67` (withHeaders); `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:63-66`; `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:42-45`

Both test fixtures configure a wibble: wobble header, but no test checks that it arrives. Header names must be valid ASCII metadata keys, otherwise gRPC throws IllegalArgumentException.

### REQ-CLIENT-CONFIG-013 Audit annotations on requests

The caller can derive a PDP client that attaches annotations to the request context of every check, batch check and plan request, where they show up in the Cerbos audit log. Passing null removes them.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:108-115` (withRequestAnnotations); `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:130-145` (check); `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:207-209` (plan); `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:242-244` (plan); `src/main/java/dev/cerbos/sdk/CheckResourcesRequestBuilder.java:28-41`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CheckResourcesRequestBuilderTest.java::CheckResourcesRequestBuilderTest::testBuild`

CerbosBlockingClientTest sets foo=bar annotations but never asserts them. testBuild only builds a batch builder with no annotations and makes no assertions. check() sets the request context twice with the same value, which is redundant but harmless.

### REQ-CLIENT-CONFIG-014 RPC failures raised as CerbosException

When the Cerbos server rejects or fails any PDP or admin call, the client throws an unchecked CerbosException. It carries the numeric gRPC status code and the status description, and its message has the form "RPC exception [<status>]".

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosException.java:10-26`; `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:153-155`; `src/main/java/dev/cerbos/sdk/CheckResourcesRequestBuilder.java:92-94`; `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:116-118`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::partialCheckRequest`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::partialPlanRequest`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::deletePolicyWithDependents`
- Conditions: operation in {check, batch check, plan, admin call}

| operation | Expected | Tests |
|---|---|---|
| check | CerbosException with INVALID_ARGUMENT for a principal without roles and a resource without id | 1 |
| plan | CerbosException with INVALID_ARGUMENT | 1 |
| admin call | CerbosException | 1 |
| batch check | CerbosException | 0 |

The exception's cause is the cause of the gRPC StatusRuntimeException, not the StatusRuntimeException itself, and that cause is usually null. Callers therefore lose the original exception and its trailers.
- Open question: Should CerbosException keep the StatusRuntimeException as its cause, so callers can read trailers and error details?

## Authorisation checks (`CHECK`)

Single and batch permission checks, the request builders for principals and resources, and reading decisions, metadata and outputs.

| ID | Requirement | Kind | Risk | Status | Confidence |
|---|---|---|---|---|---|
| REQ-CHECK-001 | Check one resource for several actions | behaviour | high | implemented-tested | high |
| REQ-CHECK-002 | Allowed only on an explicit ALLOW | authorisation | high | implemented-tested | high |
| REQ-CHECK-003 | Single check with an unexpected result count | error-handling | medium | implemented-untested | medium |
| REQ-CHECK-004 | All decisions as a map | behaviour | medium | implemented-untested | medium |
| REQ-CHECK-005 | Pass a JWT as auxiliary data | integration | high | implemented-tested | high |
| REQ-CHECK-006 | Batch check many resources in one request | behaviour | high | implemented-tested | high |
| REQ-CHECK-007 | Find a batch result by resource id | behaviour | medium | implemented-tested | high |
| REQ-CHECK-008 | Decision metadata on request | behaviour | medium | implemented-tested | high |
| REQ-CHECK-009 | Policy outputs per result | behaviour | medium | implemented-tested | high |
| REQ-CHECK-010 | Schema validation errors on check results | validation | medium | implemented-untested | medium |
| REQ-CHECK-011 | New resources default to id _NEW_ | data | low | implemented-untested | medium |
| REQ-CHECK-012 | Principal and resource attributes | data | medium | implemented-tested | high |
| REQ-CHECK-013 | Request ids and call ids | data | low | implemented-untested | medium |

### REQ-CHECK-001 Check one resource for several actions

A caller can ask whether a principal may perform one or more actions on a single resource. The SDK sends one CheckResources request with a fresh request id and returns a decision for each requested action.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:127-156` (CerbosBlockingClient.check)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithoutJWT`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithJWT`

### REQ-CHECK-002 Allowed only on an explicit ALLOW

isAllowed(action) returns true only when the server's effect for that action is ALLOW. It returns false for DENY, for an action that is not in the response, and when the result has no entry at all.

- Implemented by: `src/main/java/dev/cerbos/sdk/CheckResult.java:45-52` (CheckResult.isAllowed)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithoutJWT`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`
- Conditions: effect in {ALLOW, DENY, action absent, no result entry}

| effect | Expected | Tests |
|---|---|---|
| ALLOW | true | 2 |
| DENY | false | 2 |
| action absent | false (fail closed) | 1 |
| no result entry | false | 0 |

checkResources asks for defer on XX225 even though that resource did not request the action, and asserts false. That covers the absent-action case.

### REQ-CHECK-003 Single check with an unexpected result count

When the server's response to a single check does not contain exactly one result, the SDK returns a result with no entry. isAllowed is false for every action, getAll is empty and there are no validation errors, but getMeta() and getOutputs() throw NullPointerException.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:149-152`; `src/main/java/dev/cerbos/sdk/CheckResult.java:46-48`; `src/main/java/dev/cerbos/sdk/CheckResult.java:59-62`; `src/main/java/dev/cerbos/sdk/CheckResult.java:75-94`; `src/main/java/dev/cerbos/sdk/CheckResult.java:101-107` (getMeta/getOutputs)
- Conditions: result_count in {0, 1, >1}

| result_count | Expected | Tests |
|---|---|---|
| 1 | normal result | 1 |
| 0 | all actions denied; getMeta/getOutputs throw NullPointerException | 0 |
| >1 | all actions denied; getMeta/getOutputs throw NullPointerException | 0 |
- Open question: Should getMeta() and getOutputs() return empty values when there is no result entry, as the other accessors do?

### REQ-CHECK-004 All decisions as a map

getAll() returns an unmodifiable map from each action in the result to true when the action is allowed and false otherwise. It returns an empty map when there is no result entry.

- Implemented by: `src/main/java/dev/cerbos/sdk/CheckResult.java:59-68` (CheckResult.getAll)

### REQ-CHECK-005 Pass a JWT as auxiliary data

A caller can attach a JWT, with an optional key set id, as auxiliary data. The SDK sends it with every check, batch check and plan request so policies can use its claims. A client without auxiliary data sends an empty AuxData.

- Implemented by: `src/main/java/dev/cerbos/sdk/builders/AuxData.java:17-37`; `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:74-76` (with(AuxData)); `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:128-129`; `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:164-170`; `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:195-196`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithJWT`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`
- Conditions: aux_data in {none, jwt, jwt with key set id}

| aux_data | Expected | Tests |
|---|---|---|
| jwt | JWT claims available to policy; defer allowed | 2 |
| none | empty AuxData sent | 1 |
| jwt with key set id | JWT and key set id sent | 0 |

### REQ-CHECK-006 Batch check many resources in one request

A caller can check several resources, each with its own actions, for one principal in a single request. Resources are added either as a resource plus actions or as ResourceAction objects. The batch uses the client's auxiliary data unless the caller passes auxiliary data for that batch, which then replaces it.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:164-182` (batch); `src/main/java/dev/cerbos/sdk/CheckResourcesRequestBuilder.java:28-95`; `src/main/java/dev/cerbos/sdk/builders/ResourceAction.java:16-62`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`
  - `src/test/java/dev/cerbos/sdk/CheckResourcesRequestBuilderTest.java::CheckResourcesRequestBuilderTest::testBuild`
- Conditions: add_method in {addResources(ResourceAction), addResourceAndActions}; aux_data_source in {client, batch argument}

| add_method | aux_data_source | Expected | Tests |
|---|---|---|---|
| addResources(ResourceAction) | client | one request, per-resource results | 1 |
| addResourceAndActions | client | one request, per-resource results | 0 |
| addResources(ResourceAction) | batch argument | batch aux data used instead of client aux data | 0 |

testBuild (in the uncommitted CheckResourcesRequestBuilderTest) only constructs the builder and asserts nothing. The request id is generated when the builder is created, so calling check() twice on one builder sends the same request id both times.

### REQ-CHECK-007 Find a batch result by resource id

find(resourceId) returns the first result whose resource id matches, or empty when there is none. An optional predicate on the resource (for example its kind) can narrow the match.

- Implemented by: `src/main/java/dev/cerbos/sdk/CheckResourcesResult.java:25-46` (CheckResourcesResult.find); `src/main/java/dev/cerbos/sdk/CheckResourcesResult.java:21-23` (results)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`
- Conditions: id_match in {match, no match}; predicate in {none, passes, fails}

| id_match | predicate | Expected | Tests |
|---|---|---|---|
| match | none | result returned | 1 |
| no match | none | empty | 1 |
| match | passes | result returned | 0 |
| match | fails | empty | 0 |

With two resources of different kinds that share an id, find(id) with no predicate returns whichever comes first.

### REQ-CHECK-008 Decision metadata on request

When the caller asks for metadata, each result exposes the effective derived roles and, for each action, the policy that matched. getInfoForAction returns empty for an action with no metadata.

- Implemented by: `src/main/java/dev/cerbos/sdk/CheckResourcesRequestBuilder.java:76-79` (withIncludeMeta); `src/main/java/dev/cerbos/sdk/CheckResult.java:113-149` (CheckResult.Meta)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`
- Conditions: include_meta in {on, off}; action_in_meta in {yes, no}

| include_meta | action_in_meta | Expected | Tests |
|---|---|---|---|
| on | yes | derived roles and matched policy returned | 1 |
| on | no | getInfoForAction empty | 1 |
| off |  | metadata empty | 0 |

The single-resource check() cannot request metadata. Only the batch builder has withIncludeMeta().

### REQ-CHECK-009 Policy outputs per result

Each result exposes the outputs produced by policy rules as a map keyed by the rule's source identifier, along with a count.

- Implemented by: `src/main/java/dev/cerbos/sdk/CheckResult.java:105-107`; `src/main/java/dev/cerbos/sdk/CheckResult.java:151-179` (CheckResult.Outputs)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`

asMap() collects into an unmodifiable map, so two outputs with the same source would throw IllegalStateException.

### REQ-CHECK-010 Schema validation errors on check results

A single result reports whether the server found schema validation errors and lists them. A batch result reports whether any of its results has validation errors.

- Implemented by: `src/main/java/dev/cerbos/sdk/CheckResult.java:75-94`; `src/main/java/dev/cerbos/sdk/CheckResourcesResult.java:48-50`
- Conditions: validation_errors in {none, some}

| validation_errors | Expected | Tests |
|---|---|---|
| none | hasValidationErrors false | 0 |
| some | hasValidationErrors true, errors listed | 0 |

Only the plan path has a validation-error test (planResourcesValidation).

### REQ-CHECK-011 New resources default to id _NEW_

A resource created with a kind but no id is sent with the id "_NEW_".

- Implemented by: `src/main/java/dev/cerbos/sdk/builders/Resource.java:19-21`; `src/main/java/dev/cerbos/sdk/builders/ResourceAction.java:24-26`

### REQ-CHECK-012 Principal and resource attributes

Principals and resources carry an id, roles (principals only, and additive across calls), policy version, scope and attributes. Attribute values can be strings, numbers (sent as doubles), booleans, lists or maps, nested to any depth.

- Implemented by: `src/main/java/dev/cerbos/sdk/builders/AttributeValue.java:16-56`; `src/main/java/dev/cerbos/sdk/builders/Principal.java:13-51`; `src/main/java/dev/cerbos/sdk/builders/Resource.java:12-49`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithoutJWT`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkResources`
- Conditions: attribute_type in {string, double, bool, list, map}

| attribute_type | Expected | Tests |
|---|---|---|
| string | sent as string value | 1 |
| double | sent as number | 0 |
| bool | sent as bool | 0 |
| list | sent as list | 0 |
| map | sent as struct | 0 |

Tests only use string attributes and a single role. Scope is never set in the check tests.

### REQ-CHECK-013 Request ids and call ids

Each check, batch and plan request gets a random UUID request id. Results expose the request id and the Cerbos call id that the server returned.

- Implemented by: `src/main/java/dev/cerbos/sdk/RequestId.java:10-15`; `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:133-133`; `src/main/java/dev/cerbos/sdk/CheckResult.java:31-37`; `src/main/java/dev/cerbos/sdk/CheckResourcesResult.java:56-62`; `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:67-73`

## Query planning (`PLAN`)

PlanResources calls that tell the caller which resources a principal may act on, and how to read the plan.

| ID | Requirement | Kind | Risk | Status | Confidence |
|---|---|---|---|---|---|
| REQ-PLAN-001 | Plan a query filter for one action | behaviour | high | implemented-tested | high |
| REQ-PLAN-002 | Plan a query filter for several actions | behaviour | high | implemented-tested | high |
| REQ-PLAN-003 | Plan outcome kinds | authorisation | high | implemented-tested | high |
| REQ-PLAN-004 | Plan condition is always present | behaviour | medium | implemented-tested | medium |
| REQ-PLAN-005 | Schema validation errors on plans | validation | medium | implemented-tested | high |
| REQ-PLAN-006 | Plan result identifies the plan | data | low | implemented-tested | high |

### REQ-PLAN-001 Plan a query filter for one action

A caller can ask which resources of a kind a principal may perform one action on. The SDK sends a PlanResources request with the resource kind, policy version, scope and attributes (any resource id is dropped) and returns the plan for that action.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:194-217` (plan(String)); `src/main/java/dev/cerbos/sdk/builders/Resource.java:51-59` (toPlanResource); `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:22-25`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResources`

Uses the deprecated single action field of the request.

### REQ-PLAN-002 Plan a query filter for several actions

A caller can plan for several actions at once. The SDK sends them as the request's action list, and the result lists the same actions.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingClient.java:229-252` (plan(Iterable)); `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:27-29`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesMultipleActions`

### REQ-PLAN-003 Plan outcome kinds

A plan result is exactly one of always allowed, always denied or conditional, as reported by the server's filter kind.

- Implemented by: `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:39-49`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResources`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesMultipleActions`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesValidation`
- Conditions: filter_kind in {ALWAYS_ALLOWED, ALWAYS_DENIED, CONDITIONAL}

| filter_kind | Expected | Tests |
|---|---|---|
| CONDITIONAL | isConditional true, others false | 2 |
| ALWAYS_DENIED | isAlwaysDenied true, others false | 1 |
| ALWAYS_ALLOWED | isAlwaysAllowed true, others false | 0 |

### REQ-PLAN-004 Plan condition is always present

getCondition() always returns a present value. For a conditional plan it is the filter expression tree. For always-allowed and always-denied plans it is an empty operand, not an empty Optional.

- Implemented by: `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:51-53` (getCondition)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResources`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesMultipleActions`
- Conditions: filter_kind in {CONDITIONAL, ALWAYS_ALLOWED, ALWAYS_DENIED}

| filter_kind | Expected | Tests |
|---|---|---|
| CONDITIONAL | present, expression tree | 2 |
| ALWAYS_DENIED | present, empty operand | 0 |
| ALWAYS_ALLOWED | present, empty operand | 0 |
- Open question: Should getCondition() return Optional.empty() when the plan is not conditional? Using Optional suggests that was the intent.

### REQ-PLAN-005 Schema validation errors on plans

A plan result reports whether the server found schema validation errors and lists them. In the tested case, a request that fails validation is planned as always denied.

- Implemented by: `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:55-61`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesValidation`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResources`
- Conditions: validation_errors in {none, some}

| validation_errors | Expected | Tests |
|---|---|---|
| none | hasValidationErrors false | 1 |
| some | hasValidationErrors true, 2 errors, always denied | 1 |

Whether invalid input produces always-denied depends on the server's schema enforcement setting (reject in the test config), not on the SDK.

### REQ-PLAN-006 Plan result identifies the plan

A plan result reports the resource kind, policy version, request id and Cerbos call id that the server returned.

- Implemented by: `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:31-37`; `src/main/java/dev/cerbos/sdk/PlanResourcesResult.java:67-73`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResources`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesMultipleActions`
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::planResourcesValidation`

## Admin API (`ADMIN`)

Managing policies and schemas in a mutable Cerbos policy store: bulk add or update, list, get, enable, disable, delete, purge and reload.

| ID | Requirement | Kind | Risk | Status | Confidence |
|---|---|---|---|---|---|
| REQ-ADMIN-001 | Add or update policies from JSON or objects | behaviour | high | implemented-tested | high |
| REQ-ADMIN-002 | Validate policies and schemas before queuing | validation | medium | implemented-untested | medium |
| REQ-ADMIN-003 | Send policies and schemas in batches | non-functional | medium | implemented-tested | medium |
| REQ-ADMIN-004 | Add or update JSON schemas | behaviour | high | implemented-tested | high |
| REQ-ADMIN-005 | List policy ids with filters | behaviour | medium | implemented-tested | high |
| REQ-ADMIN-006 | Get policies by id | behaviour | medium | implemented-tested | high |
| REQ-ADMIN-007 | Enable and disable policies | behaviour | high | implemented-tested | high |
| REQ-ADMIN-008 | Delete policies | behaviour | high | implemented-tested | high |
| REQ-ADMIN-009 | Purge old store revisions | data | high | implemented-tested | high |
| REQ-ADMIN-010 | List and get schemas | behaviour | medium | implemented-tested | high |
| REQ-ADMIN-011 | Delete schemas | behaviour | medium | implemented-tested | high |
| REQ-ADMIN-012 | Reload the policy store | behaviour | medium | implemented-untested | medium |

### REQ-ADMIN-001 Add or update policies from JSON or objects

An admin can queue policies as JSON text, a JSON reader or policy objects, then send them all to the policy store with one addOrUpdate call. Nothing is sent until addOrUpdate is called.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:75-77`; `src/main/java/dev/cerbos/sdk/AddOrUpdatePolicyRequestBuilder.java:40-79`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithoutFilter`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithFilter`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::getPolicy`

The admin test loads 17 policies (YAML converted to JSON) through this builder in its @BeforeAll setup. listPoliciesWithoutFilter then asserts that all 17 are present.

### REQ-ADMIN-002 Validate policies and schemas before queuing

Each policy or schema is validated against the Cerbos proto rules as it is queued. An invalid one is rejected with a checked ValidationException that lists the violations, and it is not queued.

- Implemented by: `src/main/java/dev/cerbos/sdk/AddOrUpdatePolicyRequestBuilder.java:59-63`; `src/main/java/dev/cerbos/sdk/AddOrUpdatePolicyRequestBuilder.java:72-79`; `src/main/java/dev/cerbos/sdk/AddOrUpdateSchemaRequestBuilder.java:42-48`; `src/main/java/dev/cerbos/sdk/AddOrUpdateSchemaRequestBuilder.java:71-78`; `src/main/java/dev/cerbos/sdk/validation/Validator.java:15-25`; `src/main/java/dev/cerbos/sdk/validation/ValidationException.java:13-28`
- Conditions: input in {valid, violates rules, validator error}

| input | Expected | Tests |
|---|---|---|
| valid | queued | 1 |
| violates rules | ValidationException with violations; not queued | 0 |
| validator error | ValidationException with cause and no violations | 0 |

When a list of policies or schemas is passed, the ones before the first invalid item are already queued when the exception is thrown.

### REQ-ADMIN-003 Send policies and schemas in batches

addOrUpdate sends queued policies (and, separately, schemas) in several requests. The first request holds 9 items and each later one holds up to 10. With nothing queued, no request is sent. If a request fails, the earlier batches stay applied and the rest are not sent.

- Implemented by: `src/main/java/dev/cerbos/sdk/AddOrUpdatePolicyRequestBuilder.java:86-112` (addOrUpdate); `src/main/java/dev/cerbos/sdk/AddOrUpdateSchemaRequestBuilder.java:85-111` (addOrUpdate)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithoutFilter`
- Conditions: queued_count in {0, 1-9, 10, >10}

| queued_count | Expected | Tests |
|---|---|---|
| 0 | no request | 0 |
| 1-9 | one request | 0 |
| 10 | two requests (9 + 1) | 0 |
| >10 | 17 policies sent as 9 + 8 | 1 |

The loop flushes when i % 10 == 0, before adding item i, which gives batches of 9, 10, 10, ... The requests are not atomic across batches.
- Open question: Was the intended batch size 10 for every batch? The first batch holds 9 because of an off-by-one in the flush check.

### REQ-ADMIN-004 Add or update JSON schemas

An admin can queue schemas as an id plus JSON definition (text or reader) or as schema objects, then send them with addOrUpdate.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:225-227`; `src/main/java/dev/cerbos/sdk/AddOrUpdateSchemaRequestBuilder.java:42-78`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listSchemas`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::getSchema`

### REQ-ADMIN-005 List policy ids with filters

An admin can list policy ids, either active only or including disabled ones, optionally filtered by regular expressions on name, version and scope.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:89-119`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithoutFilter`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithFilter`
- Conditions: include_disabled in {true, false}; name_regex in {set, unset}; version_regex in {set, unset}; scope_regex in {set, unset}

| include_disabled | name_regex | version_regex | scope_regex | Expected | Tests |
|---|---|---|---|---|---|
| true | unset | unset | unset | all 17 ids | 1 |
| true | set | unset | set | 3 matching ids in order | 1 |
| false | unset | unset | unset | disabled policies excluded | 0 |
| true | unset | set | unset | filtered by version | 0 |

### REQ-ADMIN-006 Get policies by id

An admin can fetch policies by id. Only policies that exist are returned, so an unknown id produces an empty list rather than an error.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:128-138` (getPolicy)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::getPolicy`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::getPolicyNonExistent`
- Conditions: policy_exists in {yes, no}

| policy_exists | Expected | Tests |
|---|---|---|
| yes | one policy returned | 1 |
| no | empty list | 1 |

### REQ-ADMIN-007 Enable and disable policies

An admin can disable or re-enable policies by id. Each call returns the number of policies it changed.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:147-176`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::enableAndDisablePolicy`

### REQ-ADMIN-008 Delete policies

An admin can delete policies by id and gets back the number deleted. Deleting a policy that other policies depend on fails with CerbosException.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:185-195` (deletePolicy)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::deletePolicyWithoutDependents`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::deletePolicyWithDependents`
- Conditions: has_dependents in {yes, no}

| has_dependents | Expected | Tests |
|---|---|---|
| no | returns 1 | 1 |
| yes | CerbosException | 1 |

The SDK does not enforce the dependents rule. The server does.

### REQ-ADMIN-009 Purge old store revisions

An admin can purge policy store revisions and gets back the number of rows removed. A positive keepLast keeps that many recent revisions. Zero or a negative value sends no keepLast, so the server purges every revision.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:206-218` (purgeStoreRevisions)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::purgeStoreRevisions`
- Conditions: keep_last in {negative, 0, positive}

| keep_last | Expected | Tests |
|---|---|---|
| 0 | all revisions purged, count > 1 | 1 |
| negative | treated as 0 | 0 |
| positive | that many revisions kept | 0 |

### REQ-ADMIN-010 List and get schemas

An admin can list all schema ids and fetch schemas by id.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:235-261`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listSchemas`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::getSchema`

### REQ-ADMIN-011 Delete schemas

An admin can delete schemas by id and gets back the number deleted.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:270-280` (deleteSchema)
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::deleteSchema`

### REQ-ADMIN-012 Reload the policy store

An admin can ask the server to reload its policy store, and can choose to wait until the reload finishes.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosBlockingAdminClient.java:288-294` (storeReload)
- Conditions: wait in {true, false}

| wait | Expected | Tests |
|---|---|---|
| true | returns after reload completes | 0 |
| false | returns immediately | 0 |

## Testcontainers support (`TEST-SUPPORT`)

The CerbosContainer helper for running a Cerbos PDP in integration tests.

| ID | Requirement | Kind | Risk | Status | Confidence |
|---|---|---|---|---|---|
| REQ-TEST-SUPPORT-001 | Cerbos test container image | configuration | low | implemented-tested | medium |
| REQ-TEST-SUPPORT-002 | Container readiness and gRPC target | integration | low | implemented-tested | medium |

### REQ-TEST-SUPPORT-001 Cerbos test container image

CerbosContainer starts the ghcr.io/cerbos/cerbos image, tagged latest by default or with a caller-supplied tag. A custom image must declare compatibility with ghcr.io/cerbos/cerbos, otherwise construction fails.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosContainer.java:13-32`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithoutJWT`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithoutFilter`
- Conditions: image in {default, custom tag, compatible custom image, incompatible image}

| image | Expected | Tests |
|---|---|---|
| custom tag | ghcr.io/cerbos/cerbos:<tag> started | 1 |
| default | ghcr.io/cerbos/cerbos:latest | 0 |
| incompatible image | IllegalStateException from assertCompatibleWith | 0 |

Both integration fixtures use the "dev" tag. Their tests exercise the container without asserting anything about it.

### REQ-TEST-SUPPORT-002 Container readiness and gRPC target

The container exposes HTTP port 3592 and gRPC port 3593, counts as ready once the log shows "Starting gRPC server", and gives a target of 127.0.0.1:<mapped gRPC port> for building clients.

- Implemented by: `src/main/java/dev/cerbos/sdk/CerbosContainer.java:16-17`; `src/main/java/dev/cerbos/sdk/CerbosContainer.java:30-44`
- Tested by:
  - `src/test/java/dev/cerbos/sdk/CerbosClientTests.java::CerbosClientTests::checkWithoutJWT`
  - `src/test/java/dev/cerbos/sdk/CerbosBlockingAdminClientTest.java::CerbosBlockingAdminClientTest::listPoliciesWithoutFilter`

Evidence comes from fixture use only.

## Open questions

- REQ-CLIENT-CONFIG-003: When both withInsecure() and withCaCertificate() are set, should insecure mode win? The code currently gives the CA certificate precedence.
- REQ-CLIENT-CONFIG-005: Should supplying a certificate without a key (or the reverse) be a configuration error instead of being silently ignored?
- REQ-CLIENT-CONFIG-014: Should CerbosException keep the StatusRuntimeException as its cause, so callers can read trailers and error details?
- REQ-CHECK-003: Should getMeta() and getOutputs() return empty values when there is no result entry, as the other accessors do?
- REQ-PLAN-004: Should getCondition() return Optional.empty() when the plan is not conditional? Using Optional suggests that was the intent.
- REQ-ADMIN-003: Was the intended batch size 10 for every batch? The first batch holds 9 because of an off-by-one in the flush check.
