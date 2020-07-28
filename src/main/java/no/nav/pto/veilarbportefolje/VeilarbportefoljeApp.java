package no.nav.pto.veilarbportefolje;

import no.nav.common.cxf.StsSecurityConstants;
import no.nav.common.utils.Credentials;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.SECURITYTOKENSERVICE_URL_PROPERTY_NAME;

@SpringBootApplication
@ServletComponentScan
public class VeilarbportefoljeApp {

    public static void main(String... args) {
        System.setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging");

        //STS
        System.setProperty(StsSecurityConstants.STS_URL_KEY, getRequiredProperty(SECURITYTOKENSERVICE_URL_PROPERTY_NAME));
        Credentials serviceUser = getCredentials("service_user");
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, serviceUser.username);
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, serviceUser.password);

        SpringApplication.run(VeilarbportefoljeApp.class, args);
    }

}
