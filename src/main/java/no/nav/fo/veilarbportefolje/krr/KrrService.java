package no.nav.fo.veilarbportefolje.krr;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import net.sf.ehcache.util.FailSafeTimer;
import no.nav.common.utils.CollectionUtils;
import no.nav.fo.veilarbportefolje.config.ClientConfig;
import no.nav.sbl.rest.RestUtils;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import java.net.SocketException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.jodah.failsafe.Failsafe.with;
import static no.nav.fo.veilarbportefolje.config.ClientConfig.*;
import static no.nav.fo.veilarbportefolje.util.SubjectUtils.getOidcToken;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class KrrService {

    public static final String DKIF_URL_PROPERTY_NAME = "DKIF_URL";
    static final int BULK_SIZE = 500;
    static final String DKIF_URL_PATH = "/api/v1/personer/kontaktinformasjon";

    private KrrRepository krrRepository;

    @Inject
    public KrrService(KrrRepository krrRepository) {
        this.krrRepository = krrRepository;
    }

    public void oppdaterDigitialKontaktinformasjon() {
        log.info("Indeksering: Starter henting av KRR informasjon...");

        krrRepository.slettKrrInformasjon();
        List<String> fodselsnummere = krrRepository.hentAlleFnrUnderOppfolging();

        oppdaterKrrInfo(fodselsnummere);

        log.info("Indeksering: Fullført henting av KRR informasjon");
    }

    public void oppdaterKrrInfo(List<String> fodselsnummere) {
        CollectionUtils
                .partition(fodselsnummere, BULK_SIZE)
                .forEach(this::oppdaterKrrKontaktinfo);
    }

    private void oppdaterKrrKontaktinfo(List<String> fodselsnummere) {
        log.info("Oppdaterer KRR for {} brukere", fodselsnummere.size());

        Optional<KrrDTO> maybeKrrDto = hentKrrKontaktInfo(fodselsnummere);

        maybeKrrDto
                .map(dto -> new ArrayList<>(dto.getKontaktinfo().values()))
                .ifPresent(krrRepository::lagreKrrKontaktInfo);

    }

    public static Optional<KrrDTO> hentKrrKontaktInfo(List<String> fodselsnummere) {
        return RestUtils.withClient(client -> {

            RetryPolicy<Optional<KrrDTO>> retryPolicy = new RetryPolicy<Optional<KrrDTO>>()
                    .withDelay(ofSeconds(getRetryDelayInSeconds()))
                    .withMaxRetries(getMaxRetries())
                    .onRetry(retry -> log.info("Retrying...", retry.getLastFailure()))
                    .onFailure(failure -> log.error("Call failed", failure.getFailure()));

            Fallback<Optional<KrrDTO>> fallbackPolicy = Fallback.of(Optional.empty());

            return with(retryPolicy, fallbackPolicy).get(() -> {

                                KrrDTO krrDto = client.target(getRequiredProperty(DKIF_URL_PROPERTY_NAME))
                                        .path(DKIF_URL_PATH)
                                        .queryParam("inkluderSikkerDigitalPost", false)
                                        .request()
                                        .header(AUTHORIZATION, "Bearer " + getOidcToken())
                                        .header("Nav-Personidenter", fodselsnummere)
                                        .get(KrrDTO.class);

                                return Optional.of(krrDto);
                            }
                    );
                }
        );
    }
}
