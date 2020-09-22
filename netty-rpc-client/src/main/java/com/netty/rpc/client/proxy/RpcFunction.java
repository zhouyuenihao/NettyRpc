package com.netty.rpc.client.proxy;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * lambda method reference
 * g-yu
 * @param <T>
 * @param <P>
 */
@FunctionalInterface
public interface RpcFunction<T, P> extends Serializable {
    /**
     * have parameter
     *
     * @param t
     * @param p
     * @return
     */
    P apply(T t, P p);

    default String getName() throws Exception {
        Method write = this.getClass().getDeclaredMethod("writeReplace");
        write.setAccessible(true);
        SerializedLambda serializedLambda = (SerializedLambda) write.invoke(this);
        return serializedLambda.getImplMethodName();
    }
}
