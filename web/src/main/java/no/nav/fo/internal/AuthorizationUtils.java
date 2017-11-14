package no.nav.fo.internal;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

import static java.lang.System.getProperty;

@Slf4j
public class AuthorizationUtils {

    public static final String AUTHENTICATE = "WWW-Authenticate";
    public static final String BASIC_REALM = "BASIC realm=\"srvveilarbportefolje\"";
    private static final String AUTHORIZATION = "Authorization";
    private static final BASE64Decoder decoder = new BASE64Decoder();

    public static boolean isBasicAuthAuthorized(HttpServletRequest request) {
        String auth = request.getHeader(AUTHORIZATION);
        if(Objects.isNull(auth) || !auth.toLowerCase().startsWith("basic ")) {
            return false;
        }

        String basicAuth = auth.substring(6);
        String basicAuthDecoded;
        try {
            basicAuthDecoded = new String(decoder.decodeBuffer(basicAuth));
        } catch (IOException e) {
            log.error("Kunne ikke decode basic auth");
            return false;
        }

        String username = basicAuthDecoded.split(":")[0].toLowerCase();
        String password = basicAuthDecoded.split(":")[1];
        String srvUsername = getProperty("no.nav.modig.security.systemuser.username").toLowerCase();
        String srvPassword = getProperty("no.nav.modig.security.systemuser.password");

        return username.equals(srvUsername) && password.equals(srvPassword);
    }

    public static void writeUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setHeader(AUTHENTICATE, BASIC_REALM);
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
