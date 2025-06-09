/*
 * Copyright 2021-2025 Zenauth Ltd.
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.cerbos.sdk;

import com.google.protobuf.ByteString;
import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.schema.SchemaOuterClass;
import dev.cerbos.api.v1.svc.CerbosAdminServiceGrpc;
import dev.cerbos.sdk.validation.ValidationException;
import dev.cerbos.sdk.validation.Validator;
import io.grpc.StatusRuntimeException;
import org.apache.commons.io.input.ReaderInputStream;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AddOrUpdateSchemaRequestBuilder {

    private final Supplier<CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub> clientStub;
    private final List<SchemaOuterClass.Schema> schemas = new ArrayList<>();

    AddOrUpdateSchemaRequestBuilder(Supplier<CerbosAdminServiceGrpc.CerbosAdminServiceBlockingStub> clientStub) {
        this.clientStub = clientStub;
    }

    /**
     * Add a schema to the batch.
     *
     * @param id         Schema ID
     * @param schemaJson Reader for schema JSON
     * @return this
     * @throws IOException         If the schema cannot be read
     * @throws ValidationException If the schema cannot be validated
     */
    public AddOrUpdateSchemaRequestBuilder with(String id, Reader schemaJson) throws IOException, ValidationException {
        SchemaOuterClass.Schema.Builder schemaBuilder = SchemaOuterClass.Schema.newBuilder();
        SchemaOuterClass.Schema schema = schemaBuilder.setId(id).setDefinition(ByteString.readFrom(ReaderInputStream.builder().setReader(schemaJson).get())).build();

        Validator.validate(schema);
        schemas.add(schema);
        return this;
    }

    /**
     * Add a schema to the batch.
     *
     * @param id         Schema ID
     * @param schemaJson String containing the schema JSON
     * @return this
     * @throws IOException         If the schema cannot be read
     * @throws ValidationException If the schema cannot be validated
     */
    public AddOrUpdateSchemaRequestBuilder with(String id, String schemaJson) throws IOException, ValidationException {
        return with(id, new StringReader(schemaJson));
    }

    /**
     * Add a list of schemas to the batch.
     *
     * @param schemaList list of {@link dev.cerbos.api.v1.schema.SchemaOuterClass.Schema}
     * @return this
     * @throws ValidationException if any of the schemas is invalid
     */
    public AddOrUpdateSchemaRequestBuilder with(Iterable<SchemaOuterClass.Schema> schemaList) throws ValidationException {
        for (SchemaOuterClass.Schema s : schemaList) {
            Validator.validate(s);
            schemas.add(s);
        }

        return this;
    }

    /**
     * Execute the addOrUpdate call
     *
     * @throws CerbosException is the call fails
     */
    public void addOrUpdate() {
        Request.AddOrUpdateSchemaRequest.Builder batch = Request.AddOrUpdateSchemaRequest.newBuilder();
        int batchSize = 0;
        for (int i = 1; i <= schemas.size(); i++) {
            if (i % 10 == 0) {
                try {
                    clientStub.get().addOrUpdateSchema(batch.build());
                } catch (StatusRuntimeException sre) {
                    throw new CerbosException(sre.getStatus(), sre.getCause());
                }

                batch = Request.AddOrUpdateSchemaRequest.newBuilder();
                batchSize = 0;
            }

            batch.addSchemas(schemas.get(i - 1));
            batchSize++;
        }

        if (batchSize > 0) {
            try {
                clientStub.get().addOrUpdateSchema(batch.build());
            } catch (StatusRuntimeException sre) {
                throw new CerbosException(sre.getStatus(), sre.getCause());
            }
        }
    }
}
