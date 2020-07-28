package no.nav.pto.veilarbportefolje.client;

import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;

public class RestClientUtils {

        public static String authHeaderMedInnloggetBruker() {
            return SubjectHandler.getSsoToken()
                    .map(SsoToken::getToken)
                    .map(RestClientUtils::createBearerToken)
                    .orElseThrow(() -> new RuntimeException("Fant ikke token til innlogget bruker"));
        }

        public static String createBearerToken(String token) {
            return "Bearer " + token;
        }

}
