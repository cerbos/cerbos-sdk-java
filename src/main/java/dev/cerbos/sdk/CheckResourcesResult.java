/*
 * Copyright (c) 2021 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.api.v1.response.Response;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CheckResourcesResult {
    private final Response.CheckResourcesResponse resp;

    CheckResourcesResult(Response.CheckResourcesResponse resp) {
        this.resp = resp;
    }

    public Stream<CheckResult> results() {
        return this.resp.getResultsList().stream().map( entry -> new CheckResult(resp.getRequestId(), resp.getCerbosCallId(), entry));
    }

    public Optional<CheckResult> find(String resourceID) {
        return find(resourceID, null);
    }

    public Optional<CheckResult> find(
            String resourceID,
            Predicate<Response.CheckResourcesResponse.ResultEntry.Resource> predicate) {
        return this.resp.getResultsList().stream()
                .filter(
                        re -> {
                            if (!re.getResource().getId().equals(resourceID)) {
                                return false;
                            }
                            if (predicate != null) {
                                return predicate.test(re.getResource());
                            }

                            return true;
                        })
                .map(entry -> new CheckResult(resp.getRequestId(), resp.getCerbosCallId(), entry))
                .findFirst();
    }

    public boolean hasValidationErrors() {
        return resp.getResultsList().stream().anyMatch(re -> re.getValidationErrorsCount() > 0);
    }

    public Response.CheckResourcesResponse getRaw() {
        return resp;
    }

    public String getRequestId() {
        return this.resp.getRequestId();
    }

    public String getCerbosCallId() {
        return this.resp.getCerbosCallId();
    }
}
