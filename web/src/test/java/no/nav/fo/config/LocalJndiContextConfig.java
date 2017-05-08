package no.nav.fo.config;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;

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

            ctx.bind("java:/jboss/datasources/veilarbportefoljeDB", createDataSource());

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
}
