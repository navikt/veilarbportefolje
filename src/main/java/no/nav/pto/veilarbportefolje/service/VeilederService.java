package no.nav.pto.veilarbportefolje.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.sbl.rest.RestUtils;
import org.springframework.cache.annotation.Cacheable;

import javax.ws.rs.core.GenericType;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.pto.veilarbportefolje.config.CacheConfig.VEILARBVEILEDER;
import static no.nav.sbl.util.EnvironmentUtils.requireNamespace;

@Slf4j
public class VeilederService {

    private final static String VEILEDER_URL =
            format("http://veilarbveileder.%s.svc.nais.local/veilarbveileder/", requireNamespace());

    private final static String BEARER = "Bearer ";
    private final static String SEPARATOR = " ";

    @Cacheable(VEILARBVEILEDER)
    public List<VeilederId> hentVeilederePaaEnhet(String enhet) {
        String path = format("/enhet/%s/identer", enhet);

        String ssoToken = SubjectHandler.getSsoToken(SsoToken.Type.OIDC).orElseThrow(IllegalStateException::new);

        return RestUtils.withClient(client -> {
            return client
                    .target(VEILEDER_URL + path)
                    .request()
                    .header(AUTHORIZATION, BEARER + SEPARATOR + ssoToken)
                    .get(new GenericType<List<String>>() {})
                    .stream()
                    .map(VeilederId::new)
                    .collect(toList());
        });
    }
}
