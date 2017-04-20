package no.nav.fo.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class LocalJndiContextConfig {
    public static void setupJndiLocalContext() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

        try {
            InitialContext ctx = new InitialContext();
            ctx.createSubcontext("java:/");
            ctx.createSubcontext("java:/jboss/");
            ctx.createSubcontext("java:/jboss/datasources/");
            ctx.createSubcontext("java:/jboss/jms/");

            ctx.bind("java:/jboss/datasources/veilarbportefoljeDB", createDataSource());
            ctx.bind("java:jboss/mqConnectionFactory", createConnectionFactory());
            ctx.bind("java:jboss/jms/endreVeilederKo", createQueue());

        } catch (NamingException e) {

        }
    }

    private static DataSource createDataSource() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setUrl("jdbc:oracle:thin:@d26dbfl020.test.local:1521/VEILARBPORTEFOLJE_T5");
        ds.setUsername("VEILARBPORTEFOLJE");
        ds.setPassword("Ham5IcBNBZ1s");
        ds.setSuppressClose(true);
        return ds;
    }

    private static ConnectionFactory createConnectionFactory() {
        return new ActiveMQConnectionFactory("tcp://localhost:61616");
    }

    private static Destination createQueue() {
        return new ActiveMQQueue("portefolje");
    }
}
