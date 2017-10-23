package no.nav.fo;

import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.DevelopmentSecurity.ISSOSecurityConfig;
import no.nav.dialogarena.config.fasit.DbCredentials;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.eclipse.jetty.plus.jndi.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static no.nav.fo.config.LocalJndiContextConfig.*;
import static no.nav.modig.lang.collections.FactoryUtils.gotKeypress;
import static no.nav.modig.lang.collections.RunnableUtils.first;
import static no.nav.modig.lang.collections.RunnableUtils.waitFor;

public class StartJettyVeilArbPortefolje {

    public static final String APPLICATION_NAME = "veilarbportefolje";
    public static final String SERVICE_USER_NAME = "srvveilarbportefolje";

    public static void main(String[] args) throws Exception {
        SystemProperties.setFrom("veilarbportefolje.properties");

        DriverManagerDataSource dataSource  = (parseBoolean(getProperty("lokal.database"))) ? 
                setupInMemoryDatabase() : 
                    setupDataSourceWithCredentials(createDbCredentials());
        
        setServiceUserCredentials(FasitUtils.getServiceUser(SERVICE_USER_NAME, APPLICATION_NAME));

        // TODO slett når common-jetty registerer datasource fornuftig
        new Resource(DatabaseConfig.JNDI_NAME, dataSource);

        //Må ha https for csrf-token
        Jetty jetty = DevelopmentSecurity.setupISSO(Jetty.usingWar()
                .at("veilarbportefolje")
                .sslPort(9594)
                .port(9595)
                .addDatasource(dataSource, DatabaseConfig.JNDI_NAME)
                .overrideWebXml(), new ISSOSecurityConfig(APPLICATION_NAME))
                .configureForJaspic()
                .buildJetty();

        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    private static DbCredentials createDbCredentials() {
        return (getProperty("database.url") == null)  ? FasitUtils.getDbCredentials(APPLICATION_NAME) :
                new DbCredentials()
                .setUrl(getProperty("database.url"))
                .setUsername(getProperty("database.brukernavn", "sa"))
                .setPassword(getProperty("database.passord", ""));
    }
    
}
