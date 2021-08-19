package dev.cerbos.sdk;

import dev.cerbos.api.v1.request.Request;
import dev.cerbos.api.v1.response.Response;
import dev.cerbos.api.v1.svc.CerbosServiceGrpc;
import io.grpc.Channel;

import java.util.concurrent.TimeUnit;

public class CerbosBlockingClient {
    private final CerbosServiceGrpc.CerbosServiceBlockingStub cerbosStub;
    private final long timeoutMillis;

    CerbosBlockingClient(Channel channel, long timeoutMillis) {
        this.cerbosStub = CerbosServiceGrpc.newBlockingStub(channel);
        this.timeoutMillis = timeoutMillis;
    }

    private CerbosServiceGrpc.CerbosServiceBlockingStub withClient() {
        return cerbosStub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public String version() {
        Response.ServerInfoResponse response = withClient().serverInfo(Request.ServerInfoRequest.newBuilder().build());
        return String.format("%s (%s built on %s)", response.getVersion(), response.getCommit(), response.getBuildDate());
    }
}
