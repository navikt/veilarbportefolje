package no.nav.fo.database.testdriver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;

import static no.nav.fo.util.ProxyUtils.proxy;
import static no.nav.fo.util.ReflectionUtils.getMethod;

public class ConnectionInvocationHandler implements InvocationHandler {

    private static final Method CREATE_STATEMENT_METHOD = getMethod(Connection.class, "createStatement");

    private final Connection connection;

    ConnectionInvocationHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object realResult = method.invoke(connection, args);
        if (CREATE_STATEMENT_METHOD.equals(method)) {
            return proxy(new StatementInvocationHandler((Statement) realResult), Statement.class);
        }
        return realResult;
    }

}
