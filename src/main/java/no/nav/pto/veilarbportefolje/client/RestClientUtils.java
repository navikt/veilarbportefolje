package no.nav.pto.veilarbportefolje.client;

import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;

public class RestClientUtils {

        public static String authHeaderMedSystemBruker() {
            return SubjectHandler.getSsoToken(SsoToken.Type.OIDC)
                    .map(RestClientUtils::createBearerToken)
                    .orElseThrow(IllegalStateException::new);
        }

        public static String createBearerToken(String token) {
            return "Bearer " + token;
        }

}
