package no.nav.fo.util;

import no.nav.brukerdialog.security.domain.OidcCredential;

import javax.security.auth.Subject;

public class TokenUtils {

    public static String getTokenBody(Subject subject){
        if(subject.getPublicCredentials().isEmpty()) {
            return null;
        }
        OidcCredential credential = (OidcCredential) subject
            .getPublicCredentials()
            .stream()
            .filter(cred -> cred instanceof OidcCredential).findFirst()
            .get();

        return credential.getToken();
    }
}
