package no.nav.fo.util;

import no.nav.brukerdialog.security.domain.OidcCredential;

import javax.security.auth.Subject;

public class TokenUtils {

    public static String getTokenBody(Subject subject) {
        return subject
                .getPublicCredentials()
                .stream()
                .filter(cred -> cred instanceof OidcCredential)
                .map(c -> (OidcCredential) c)
                .map(OidcCredential::getToken)
                .findFirst()
                .orElse(null);
    }

}
