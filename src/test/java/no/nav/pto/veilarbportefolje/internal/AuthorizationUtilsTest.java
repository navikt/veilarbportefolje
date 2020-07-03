package no.nav.pto.veilarbportefolje.internal;


import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Base64;

import static java.lang.System.setProperty;
import static no.nav.pto.veilarbportefolje.internal.AuthorizationUtils.AUTHORIZATION;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationUtilsTest {

    @Test
    public void skalIkkeGodtaRequest() {
        setProperty(SYSTEMUSER_USERNAME, "dontcare");
        setProperty(SYSTEMUSER_PASSWORD, "dontcare");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(AUTHORIZATION)).thenReturn("");
        assertThat(AuthorizationUtils.isBasicAuthAuthorized(request)).isFalse();
    }

    @Test
    public void skalGodtaRequest() {
        Base64.Encoder encoder = Base64.getEncoder();

        setProperty(SYSTEMUSER_USERNAME, "brukernavn");
        setProperty(SYSTEMUSER_PASSWORD, "passord");

        String basicAuth = "brukernavn:passord";
        String basicAuthDecoded = new String(encoder.encode(basicAuth.getBytes()));
        String authHeader = "Basic " + basicAuthDecoded;

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(AUTHORIZATION)).thenReturn(authHeader);

        assertThat(AuthorizationUtils.isBasicAuthAuthorized(request)).isTrue();
    }
}
