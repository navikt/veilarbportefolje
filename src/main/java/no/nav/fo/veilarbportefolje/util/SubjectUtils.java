package no.nav.fo.veilarbportefolje.util;

import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;

public class SubjectUtils {
    public static String getOidcToken() {
        Subject subject = SubjectHandler.getSubject().orElseThrow(() -> new IllegalStateException("Cannot find subject"));
        return subject.getSsoToken(SsoToken.Type.OIDC).orElseThrow(() -> new IllegalStateException("Cannot find OIDC-token for subject"));
    }

}
