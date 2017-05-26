package no.nav.fo.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ProxyUtils {

    public static <T> T proxy(InvocationHandler invocationHandler, Class<T> proxyClass) {
        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{proxyClass},
                invocationHandler
        );
    }

}
