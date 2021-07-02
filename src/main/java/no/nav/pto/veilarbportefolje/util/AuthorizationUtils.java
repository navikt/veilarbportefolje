package no.nav.pto.veilarbportefolje.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;


@Slf4j
public class AuthorizationUtils {

    private static final String AUTHENTICATE = "WWW-Authenticate";
    private static final String BASIC_REALM = "BASIC realm=\"srvveilarbportefolje\"";
    static final String AUTHORIZATION = "Authorization";
    private static final Base64.Decoder decoder = Base64.getDecoder();

    public static boolean isBasicAuthAuthorized(HttpServletRequest request, Credentials srvBrukere) {
        String auth = request.getHeader(AUTHORIZATION);
        if (Objects.isNull(auth) || !auth.toLowerCase().startsWith("basic ")) {
            return false;
        }

        String basicAuth = auth.substring(6);
        String basicAuthDecoded = new String(decoder.decode(basicAuth));

        String username = basicAuthDecoded.split(":")[0].toLowerCase();
        String password = basicAuthDecoded.split(":")[1];
        return username.equals(srvBrukere.username) && password.equals(srvBrukere.password);
    }

    public static void writeUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setHeader(AUTHENTICATE, BASIC_REALM);
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
