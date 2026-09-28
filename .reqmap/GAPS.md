# Gap analysis: cerbos-sdk-java

Code at `6ba1f6844beb`, requirements model at `6ba1f6844beb`, generated 2026-09-28.
Inputs: tests.json yes, coverage.json yes with per-test contexts (JaCoCo, one slice per test method), churn window 90 days.
Cerbos Hub (`dev.cerbos.sdk.hub`, its tests and `service_config.json`) is out of scope. Its requirements, tests and coverage are not part of this report.
The working tree has one uncommitted test file (`CheckResourcesRequestBuilderTest.java`), which is included in both tests.json and coverage.json.

## Summary

29 of 47 requirements have a mapped test and 18 have none. Line coverage of the SDK is 65.0% (353/543) and branch coverage is 43.3% (39/90). The gaps cluster in client configuration. Every integration test builds a plaintext client, so none of the TLS, CA, mutual TLS or insecure-mode code runs under test, and two of those paths behave in surprising ways. That is the most important gap.

| Status | Requirements |
|---|---|
| implemented-tested | 29 |
| implemented-untested | 18 |
| tested-only | 0 |
| documented-only | 0 |

| | High risk | Medium risk | Low risk |
|---|---|---|---|
| Confirmed gaps | 12 | 13 | 8 |

## Confirmed gaps

### REQ-CLIENT-CONFIG-002 to 005: TLS, insecure mode, custom CA and mutual TLS (UNTESTED, high risk)

Every fixture calls `withPlaintext()`, so the TLS branch of `CerbosClientBuilder.build()` never runs (lines 85-111 are uncovered apart from the plaintext arm). Nothing fails if TLS stops working, if the CA file is ignored, or if a client certificate is not presented. Two behaviours recorded in the model are also unchecked: a CA certificate silently overrides insecure mode, and a certificate without its key (or a key without its certificate) is ignored without an error.

Evidence: `src/main/java/dev/cerbos/sdk/CerbosClientBuilder.java:35-63`, `:85-111`; no tests; 19 test contexts execute the builder, all through the plaintext arm. Fixture certificates already exist in `src/test/resources/certificates`.

Suggested tests (Cerbos container started with TLS using the fixture certificates):

| Case | plaintext | insecure | CA cert | client cert/key | Expect |
|---|---|---|---|---|---|
| `tlsWithCustomCa` | off | off | fixture CA | none | check succeeds |
| `tlsWithoutCaFails` | off | off | unset | none | CerbosException, UNAVAILABLE |
| `insecureTrustsAnyCert` | off | on | unset | none | check succeeds |
| `caOverridesInsecure` | off | on | wrong CA | none | CerbosException (documents current behaviour) |
| `mutualTls` | off | off | fixture CA | both set | check succeeds against a server that requires client certs |
| `certWithoutKeyIgnored` | off | off | fixture CA | cert only | builds, no client cert presented |
| `unreadableCaFails` | off | off | missing file | none | InvalidClientConfigurationException "Failed to set CA trust root" |

Add to: `src/test/java/dev/cerbos/sdk/CerbosBlockingClientTest.java` (or a new `CerbosTlsClientTest`), following the container setup in its `@BeforeAll`.

### REQ-CLIENT-CONFIG-010: Admin client uses Basic credentials (UNTESTED_VALUES, high risk)

The tests prove that the right credentials work. Nothing checks that a null username or password fails at build time, or that wrong credentials are rejected by the server as a CerbosException.

Evidence: `CerbosClientBuilder.java:138-145`, `AdminApiCredentials.java:15-34`; line 141 (the null check) is uncovered.

| Case | username | password | Expect |
|---|---|---|---|
| `adminClientNullUsername` | null | "x" | InvalidClientConfigurationException |
| `adminClientNullPassword` | "x" | null | InvalidClientConfigurationException |
| `adminClientWrongPassword` | "cerbos" | "wrong" | CerbosException, UNAUTHENTICATED |

Add to: `CerbosBlockingAdminClientTest.java`, reusing its container.

### REQ-CLIENT-CONFIG-014: RPC failures raised as CerbosException (SCENARIO_NO_TEST, high risk)

The batch check error path (`CheckResourcesRequestBuilder.java:92-94`) never runs, and neither do the catch blocks in most admin methods (list, enable, disable, purge, reload). No test checks what the exception carries. The model records that its cause is the StatusRuntimeException's cause, which is usually null, so the original status exception is lost.

Evidence: `CerbosException.java:10-26`, `CheckResourcesRequestBuilder.java:92-94`, `CerbosBlockingAdminClient.java:116-118`; tests cover single check, plan and delete errors only.

| Case | Operation | Expect |
|---|---|---|
| `batchCheckInvalidRequest` | batch check with a resource missing its kind | CerbosException with INVALID_ARGUMENT |
| `cerbosExceptionCarriesStatus` | any failing call | `getStatus().getCode()` matches; message starts "RPC exception" |

Add to: `CerbosClientTests.java` next to `partialCheckRequest`.

### REQ-CHECK-006: Batch check many resources in one request (SCENARIO_NO_TEST, high risk)

`addResourceAndActions` is never called, and no test passes aux data to the batch builder, so the rule that batch aux data replaces client aux data is unchecked.

Evidence: `CheckResourcesRequestBuilder.java:51-56` uncovered; tested by `checkResources` only.

| Case | Add method | Aux data source | Expect |
|---|---|---|---|
| `batchAddResourceAndActions` | addResourceAndActions | client | per-resource results match single checks |
| `batchAuxDataOverridesClient` | addResources(ResourceAction) | batch argument | decision uses the batch JWT |

Add to: `CerbosClientTests.java`, following `checkResources`.

### REQ-ADMIN-009: Purge old store revisions (UNTESTED_VALUES, high risk)

Only `keepLast` 0 is tested. A positive value (keep that many revisions) and a negative value (treated as 0, which purges everything) are both unchecked. Purging the wrong number of revisions loses history.

Evidence: `CerbosBlockingAdminClient.java:206-218`, line 209 uncovered; `purgeStoreRevisions`.

| Case | keepLast | Expect |
|---|---|---|
| `purgeKeepsLastN` | 1 after two updates | affected rows equal revisions minus 1 |
| `purgeNegativeKeepsNone` | -1 | same result as 0 |

Add to: `CerbosBlockingAdminClientTest.java`, following `purgeStoreRevisions`.

### REQ-CHECK-002: Allowed only on an explicit ALLOW (UNTESTED_VALUES, high risk)

`isAllowed` returns false when there is no result entry at all (`CheckResult.java:46-47`). That branch has never run. It is the fail-closed case, and a regression that returns true or throws would go unnoticed.

Evidence: `CheckResult.java:45-52`, line 47 uncovered.

Suggested test: `isAllowedWithNoEntryIsFalse`: build `CheckResult` from an empty entry, expect `isAllowed("view")` false. Add to a new `CheckResultTest.java` (pure unit test).

### REQ-CHECK-005: Pass a JWT as auxiliary data (SCENARIO_NO_TEST, high risk)

The JWT path is tested without a key set id only. Nothing checks that a key set id is sent.

Suggested test: `checkWithJWTAndKeySetId`: configure a second key set in the container, pass `AuxData.withJWT(token, "ks2")`, expect the decision that depends on a claim. Add to `CerbosClientTests.java`, following `checkWithJWT`.

### REQ-PLAN-003 and REQ-PLAN-004: Plan outcome kinds (UNTESTED_VALUES, high risk)

No test gets an ALWAYS_ALLOWED plan, so `isAlwaysAllowed()` has only ever returned false. The empty-operand condition for ALWAYS_ALLOWED and ALWAYS_DENIED is also unchecked.

Evidence: `PlanResourcesResult.java:39-53`, one arm of each comparison untaken.

Suggested test: `planResourcesAlwaysAllowed`: plan an action the principal's role always has, expect `isAlwaysAllowed()` true, the other two false, and `getCondition()` present with an empty operand. Add to `CerbosClientTests.java`, following `planResources`.

### Medium risk

Each of these has a real hole with a concrete consequence. Outlines are one line each.

- **REQ-ADMIN-003, send policies in batches.** The 17-policy fixture goes through the off-by-one batching (9 then 8), but nothing asserts batch sizes. Test with a fake stub: 10 policies should mean 2 calls today (9 + 1). Lines 93-94 and 108-109 (error paths) never run.
- **REQ-ADMIN-002, validate before queuing.** No test passes an invalid policy or schema. `addPolicyRejectsInvalid`: expect ValidationException and nothing sent. Include a list with an invalid third item to pin down the partial-queue behaviour.
- **REQ-ADMIN-005, list policy ids.** `listActivePolicies` (include_disabled false) is never called, and neither is the version filter. `listActivePoliciesExcludesDisabled`: disable one, expect 16.
- **REQ-ADMIN-012, reload the store.** No test. `reloadStoreWaits` with wait true and false, expecting no exception.
- **REQ-CLIENT-CONFIG-008, per-call deadline.** Runs on every call, never asserted. `timeoutExceededRaisesDeadline`: 1 ms timeout, expect CerbosException DEADLINE_EXCEEDED.
- **REQ-CLIENT-CONFIG-011, admin credentials from env.** No test. Needs env injection. Test the missing-variable case, which should raise InvalidClientConfigurationException.
- **REQ-CLIENT-CONFIG-013, audit annotations.** Annotations are set but never asserted, and `testBuild` has no assertions (NO_ASSERTIONS confirmed). Assert that the built request carries the annotations.
- **REQ-CHECK-003, unexpected result count.** No test for zero or several results on a single check.
- **REQ-CHECK-004, decisions as a map.** No test.
- **REQ-CHECK-007, find a batch result.** The predicate overload is never used. Add pass and fail cases, and two kinds sharing one id.
- **REQ-CHECK-008, decision metadata.** The include_meta off case is not asserted. `getMeta()` on an action with no meta entry throws (model note), so pin that down.
- **REQ-CHECK-010, validation errors on check results.** Only the plan path is tested. Mirror `planResourcesValidation` for check.
- **REQ-CHECK-012, attribute types.** Only strings are tested. Add double, bool, list and map attributes to a policy condition.

### Low risk

REQ-CLIENT-CONFIG-001 (missing target), 006 (authority override), 007 (interceptors), 009 (playground header, PlaygroundIT has no assertions), 012 (custom headers configured but never checked), REQ-CHECK-011 (`_NEW_` default id), REQ-CHECK-013 (request and call ids), REQ-TEST-SUPPORT-001 (image tag). All untested. They are cheap unit tests if anyone wants them, but a regression in any of them is unlikely to cause harm.

## Contradictions

- `CerbosBlockingAdminClientTest::listPoliciesWithoutFilter` asserts 17 policies and `getPolicyNonExistent` asserts that `resource.foo.vdefault` does not exist, but the `@BeforeAll` setup loads 18 policies including `resource.foo.vdefault`. Both pass only because JUnit happens to run `deletePolicyWithoutDependents` first. Run on their own, they fail with "expected 17 but was 18" and "expected 0 but was 1".

## Model corrections

None. Every UNTESTED candidate with incidental coverage (REQ-CLIENT-CONFIG-002, 008, REQ-ADMIN-002, REQ-CHECK-003) was checked. The tests that run that code assert something else, so the gaps are real and the mappings stand.

## Follow-ups

- Make the admin tests independent of method order: reset the store in `@BeforeEach`, or add `@TestMethodOrder` and say why. `.reqmap/raw/per_test.sh` depends on this too. It currently builds coverage for the two failing slices from the `.exec` file.
- Hub was dropped from scope for this run. The Hub model from the first pass recorded two real bugs worth keeping in mind: `Utils.filesFromDirectory` returns nothing because `Path.endsWith(".yaml")` compares path segments, and `uploadFilesFromDirectory` lets every file through for the same reason.
- The open questions in REQUIREMENTS.md are still unanswered. Several (CA versus insecure precedence, batch sizes) decide whether the suggested tests should assert current behaviour or the intended behaviour.

## Appendix: triaged as noise

| Category | Requirement | Reason |
|---|---|---|
| UNCOVERED_CODE, PARTIAL_BRANCHES (105) | various | Same cause as a confirmed gap listed above (mostly StatusRuntimeException catch blocks under REQ-CLIENT-CONFIG-014 and the TLS branches). Counted once there. |
| UNTESTED_VALUES, UNTESTED_PAIRS, SCENARIO_NO_TEST | REQ-ADMIN-005, REQ-CHECK-007 | Independent guard clauses. Covering each value once is enough, and missing pairs add little. |
