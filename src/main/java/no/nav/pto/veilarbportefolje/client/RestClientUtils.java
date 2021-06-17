package no.nav.pto.veilarbportefolje.client;

import no.nav.common.auth.context.AuthContextHolderThreadLocal;

public class RestClientUtils {

    public static String authHeaderMedSystemBruker() {
        return AuthContextHolderThreadLocal
                .instance().getIdTokenString()
                .map(RestClientUtils::createBearerToken)
                .orElseThrow(IllegalStateException::new);
    }

    public static String createBearerToken(String token) {
        return "Bearer " + token;
    }

}
