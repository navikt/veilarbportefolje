package no.nav.pto.veilarbportefolje.client;

import no.nav.common.auth.context.AuthContextHolder;

public class RestClientUtils {

    /*
    TODO:
    Usikker om denne koden får samme resultat om denne:
        SubjectHandler.getSsoToken(SsoToken.Type.OIDC)
                    .map(RestClientUtils::createBearerToken)
                    .orElseThrow(IllegalStateException::new);
     */
        public static String authHeaderMedSystemBruker() {
            return AuthContextHolder.getIdTokenString()
                    .map(RestClientUtils::createBearerToken)
                    .orElseThrow(IllegalStateException::new);
        }

        public static String createBearerToken(String token) {
            return "Bearer " + token;
        }

}
