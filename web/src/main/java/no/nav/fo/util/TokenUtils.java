package no.nav.fo.util;

import no.nav.brukerdialog.security.domain.OidcCredential;

import javax.security.auth.Subject;

public class TokenUtils {

    public static String getTokenBody(Subject subject){
        OidcCredential credential = (OidcCredential) subject
            .getPublicCredentials()
            .stream()
            .filter(cred -> cred instanceof OidcCredential).findFirst()
            .get();

        return credential.getToken();
    }
}
