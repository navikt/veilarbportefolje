package no.nav.fo.config;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JndiLocalContextConfig {
    public static void setupJndiLocalContext() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

        try {
            SingleConnectionDataSource ds = new SingleConnectionDataSource();
            ds.setUrl("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=d26dbfl007.test.local)(PORT=1521)))(CONNECT_DATA=(SERVICE_NAME=t4veilarbportefolje)(INSTANCE_NAME=cdbx01t)(UR=A)(SERVER=DEDICATED)))");
            ds.setUsername("t4_veilarbportefolje");
            ds.setPassword("XAYV4qdi1REt");
            ds.setSuppressClose(true);

            InitialContext ctx = new InitialContext();
            ctx.createSubcontext("java:/");
            ctx.createSubcontext("java:/jboss/");
            ctx.createSubcontext("java:/jboss/datasources/");

            ctx.bind("java:/jboss/datasources/veilarbportefoljeDB", ds);

        } catch (NamingException e) {

        }
    }
}
