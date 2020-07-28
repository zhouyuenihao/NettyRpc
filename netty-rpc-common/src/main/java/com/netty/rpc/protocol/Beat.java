package com.netty.rpc.protocol;

public final class Beat {

    public static final int BEAT_INTERVAL = 60;
    public static final String BEAT_ID = "BEAT_PING_PONG";

    public static RpcRequest BEAT_PING;

    static {
        BEAT_PING = new RpcRequest() {
        };
        BEAT_PING.setRequestId(BEAT_ID);
    }

}
