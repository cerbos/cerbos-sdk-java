/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;

import static dev.cerbos.sdk.builders.AttributeValue.stringValue;

public class PlaygroundIT {
    public static void main(String[] args)
            throws CerbosClientBuilder.InvalidClientConfigurationException {
        CerbosBlockingClient client =
                new CerbosClientBuilder("localhost:8000")
                        .withInsecure()
                        .withPlaygroundInstance("x")
                        .buildBlockingClient();

        CheckResult have =
                client.check(
                        Principal.newInstance("john", "employee")
                                .withPolicyVersion("20210210")
                                .withAttribute("department", stringValue("marketing"))
                                .withAttribute("geography", stringValue("GB")),
                        Resource.newInstance("leave_request", "xx125")
                                .withPolicyVersion("20210210")
                                .withAttribute("department", stringValue("marketing"))
                                .withAttribute("geography", stringValue("GB"))
                                .withAttribute("owner", stringValue("john")),
                        "view:public",
                        "approve");

        System.out.printf("view:public = %b\n", have.isAllowed("view:public"));
        System.out.printf("approve = %b\n", have.isAllowed("approve"));
    }
}
