package no.nav.fo.veilarbportefolje.internal;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.fo.veilarbportefolje.batchjob.BatchJob;
import no.nav.fo.veilarbportefolje.batchjob.RunningJob;
import no.nav.fo.veilarbportefolje.krr.KrrService;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.brukerdialog.security.domain.IdentType.Systemressurs;
import static no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProviderConfig.STS_OIDC_CONFIGURATION_URL_PROPERTY;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;

@Slf4j
public class PopulerKrrServlet extends HttpServlet {

    private KrrService krrService;

    public PopulerKrrServlet(KrrService krrService) {
        this.krrService = krrService;
    }

    @Value
    private static class StsConfiguration {
        private String token_endpoint;
    }

    @Value
    private static class StsOidcToken {
        private String access_token;
        private String token_type;
        private String expires_in;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {

            String stsUrl = EnvironmentUtils.getRequiredProperty(STS_OIDC_CONFIGURATION_URL_PROPERTY);

            StsConfiguration stsConfiguration = RestUtils.withClient(
                    c -> c.target(stsUrl)
                            .request()
                            .get(StsConfiguration.class)
            );

            StsOidcToken stsOidcToken = RestUtils.withClient(c ->
                    c.target(stsConfiguration.token_endpoint)
                            .queryParam("grant_type", "client_credentials")
                            .queryParam("scope", "openid")
                            .request()
                            .header(AUTHORIZATION, req.getHeader(AUTHORIZATION))
                            .get(StsOidcToken.class)
            );

            String systemUserName = EnvironmentUtils.getRequiredProperty(SYSTEMUSER_USERNAME).toLowerCase();

            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("token_type", stsOidcToken.token_type);
            attributes.put("expires_in", stsOidcToken.expires_in);

            SsoToken ssoToken = SsoToken.oidcToken(stsOidcToken.access_token, attributes);
            Subject subject = new Subject(systemUserName + "_oidc_token", Systemressurs, ssoToken);

            RunningJob runningJob = BatchJob.runAsyncJobWithSubject(subject, krrService::oppdaterDigitialKontaktinformasjon);

            resp.getWriter().write(String.format("Startet oppdatering av reservesjonsdata fra krr (via dkif) med jobId %s på pod %s", runningJob.getJobId(), runningJob.getPodName()));
            resp.setStatus(SC_OK);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
