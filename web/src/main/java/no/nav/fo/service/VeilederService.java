package no.nav.fo.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.VeilederId;
import no.nav.sbl.rest.RestUtils;
import org.springframework.cache.annotation.Cacheable;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.fo.config.CacheConfig.VEILARBVEILEDER;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class VeilederService {
    public static final String VEILARBVEILEDER_URL_PROPERTY = "veilarbveileder.api.url";
    private static final Client restClient = RestUtils.createClient();
    private final String host;

    public VeilederService() {
        host = getRequiredProperty(VEILARBVEILEDER_URL_PROPERTY).toLowerCase();
    }

    @Cacheable(VEILARBVEILEDER)
    public List<VeilederId> getIdenter(String enhet) {
        log.info("henter identer for enhet: {}", enhet);
        List<String> strings = restClient
                .target(String.format("%s/enhet/%s/identer", host, enhet))
                .request()
                .header(ACCEPT, APPLICATION_JSON)
                .get(new GenericType<List<String>>() {});

        return strings
                .stream()
                .map(VeilederId::new)
                .collect(Collectors.toList());
    }
}
