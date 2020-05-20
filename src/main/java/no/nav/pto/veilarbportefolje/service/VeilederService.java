package no.nav.pto.veilarbportefolje.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.sbl.rest.RestUtils;
import org.springframework.cache.annotation.Cacheable;

import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.pto.veilarbportefolje.config.CacheConfig.VEILARBVEILEDER;

@Slf4j
public class VeilederService {
    private final static String BEARER = "Bearer ";
    private final static String SEPARATOR = " ";

    private final String url;

    public VeilederService(String namespace) {
        url = format("http://veilarbveileder.%s.svc.nais.local/veilarbveileder", namespace);
    }

    @Cacheable(VEILARBVEILEDER)
    public List<VeilederId> hentVeilederePaaEnhet(String enhet) {
        String path = format("/api/enhet/%s/identer", enhet);

        String ssoToken = SubjectHandler.getSsoToken(SsoToken.Type.OIDC).orElseThrow(IllegalStateException::new);

        Supplier<List<VeilederId>> httpCall = () -> RestUtils.withClient(client -> {
            return client
                    .target(url + path)
                    .request()
                    .header(AUTHORIZATION, BEARER + SEPARATOR + ssoToken)
                    .get(new GenericType<List<String>>() {
                    })
                    .stream()
                    .map(VeilederId::new)
                    .collect(toList());
        });

        Result<List<VeilederId>> result = Result.of(httpCall);
        if (result.err().isPresent()) {
            log.error("Kall mot veilarbveileder feilet for enhet " + enhet, result.err().get());
            return emptyList();
        }

        return result.ok().orElseThrow(IllegalStateException::new);
    }
}
