package no.nav.fo.config;

import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.dialogarena.config.fasit.LdapConfig;

public class SecurityTestConfig {

    public static void setupLdap() {
        LdapConfig ldapConfig = FasitUtils.getLdapConfig("ldap", "veilarbportefolje", "t5");
        System.setProperty("ldap.username", ldapConfig.username);
        System.setProperty("ldap.password", ldapConfig.username);
    }

}
