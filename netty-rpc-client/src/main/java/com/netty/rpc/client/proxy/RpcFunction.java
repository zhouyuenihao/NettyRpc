package com.netty.rpc.client.proxy;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * lambda method reference
 * g-yu
 *
 * @param <T>
 * @param <P>
 */
@FunctionalInterface
public interface RpcFunction<T, P> extends SerializableFunction<T> {
    /**
     * have parameter
     *
     * @param t
     * @param p
     * @return
     */
    Object apply(T t, P p);


}
