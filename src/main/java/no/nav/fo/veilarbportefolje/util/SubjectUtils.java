package no.nav.fo.veilarbportefolje.util;

import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;

public class SubjectUtils {
    public static String getOidcToken() {
        Subject subject = SubjectHandler.getSubject().orElseThrow(IllegalStateException::new);
        return subject.getSsoToken(SsoToken.Type.OIDC).orElseThrow(IllegalStateException::new);
    }

}
