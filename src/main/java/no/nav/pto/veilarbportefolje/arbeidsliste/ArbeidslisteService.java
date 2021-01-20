package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;

@Service
public class ArbeidslisteService {
    private static final Logger log = LoggerFactory.getLogger(ArbeidslisteService.class);

    private final AktorregisterClient aktorregisterClient;
    private final ArbeidslisteRepository arbeidslisteRepository;
    private final BrukerService brukerService;
    private final ElasticServiceV2 elasticServiceV2;
    private final MetricsClient metricsClient;

    @Autowired
    public ArbeidslisteService(
            AktorregisterClient aktorregisterClient,
            ArbeidslisteRepository arbeidslisteRepository,
            BrukerService brukerService,
            ElasticServiceV2 elasticServiceV2, MetricsClient metricsClient
    ) {
        this.aktorregisterClient = aktorregisterClient;
        this.arbeidslisteRepository = arbeidslisteRepository;
        this.brukerService = brukerService;
        this.elasticServiceV2 = elasticServiceV2;
        this.metricsClient = metricsClient;
    }

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return hentAktoerId(fnr).map(this::getArbeidsliste).get();
    }

    public Try<Arbeidsliste> getArbeidsliste(AktoerId aktoerId) {
        return arbeidslisteRepository.retrieveArbeidsliste(aktoerId);
    }

    public Try<ArbeidslisteDTO> createArbeidsliste(ArbeidslisteDTO dto) {

        metricsClient.report((new Event("arbeidsliste.opprettet")));

        Try<AktoerId> aktoerId = hentAktoerId(dto.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        dto.setAktoerId(aktoerId.get());

        String navKontorForBruker = brukerService.hentNavKontorFraDbLinkTilArena(dto.getFnr()).orElseThrow();
        dto.setNavKontorForArbeidsliste(navKontorForBruker);

        return arbeidslisteRepository
                .insertArbeidsliste(dto).onSuccess(elasticServiceV2::updateArbeidsliste);
    }

    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        Try<AktoerId> aktoerId = hentAktoerId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }

        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerId(aktoerId.get()))
                .onSuccess(elasticServiceV2::updateArbeidsliste);
    }

    public int slettArbeidsliste(AktoerId aktoerId) {
        final int rowsUpdated = arbeidslisteRepository.slettArbeidsliste(aktoerId);
        if (rowsUpdated == 1) {
            elasticServiceV2.slettArbeidsliste(aktoerId);
        }
        return rowsUpdated;
    }

    public int slettArbeidsliste(Fnr fnr) {
        Optional<AktoerId> aktoerId = brukerService.hentAktoerId(fnr);
        if (aktoerId.isPresent()) {
            return slettArbeidsliste(aktoerId.get());
        }
        log.error("fant ikke aktørId på fnr");
        return -1;
    }

    private Try<AktoerId> hentAktoerId(Fnr fnr) {
        return Try.of(() -> AktoerId.of(aktorregisterClient.hentAktorId(fnr.toString())));
    }

    public Validation<String, List<Fnr>> erVeilederForBrukere(List<Fnr> fnrs) {
        List<Fnr> validerteFnrs = new ArrayList<>(fnrs.size());
        fnrs.forEach(fnr -> {
            if (erVeilederForBruker(fnr.toString()).isValid()) {
                validerteFnrs.add(fnr);
            }
        });

        return validerteFnrs.size() == fnrs.size() ? valid(validerteFnrs) : invalid(format("Veileder har ikke tilgang til alle brukerene i listen: %s", fnrs));

    }

    public Validation<String, Fnr> erVeilederForBruker(String fnr) {
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

        boolean erVeilederForBruker =
                ValideringsRegler
                        .validerFnr(fnr)
                        .map(validFnr -> erVeilederForBruker(validFnr, veilederId))
                        .getOrElse(false);

        if (erVeilederForBruker) {
            return valid(new Fnr(fnr));
        }
        return invalid(format("Veileder %s er ikke veileder for bruker med fnr %s", veilederId, fnr));
    }


    public Boolean erVeilederForBruker(Fnr fnr, VeilederId veilederId) {
        return hentAktoerId(fnr)
                .map(aktoerId -> erVeilederForBruker(aktoerId, veilederId))
                .getOrElse(false);
    }

    public Boolean erVeilederForBruker(AktoerId aktoerId, VeilederId veilederId) {
        return brukerService
                .hentVeilederForBruker(aktoerId)
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .orElse(false);
    }

    public Optional<String> hentNavKontorForArbeidsliste(AktoerId aktoerId) {
        return arbeidslisteRepository.hentNavKontorForArbeidsliste(aktoerId);
    }

    public boolean brukerHarByttetNavKontor(AktoerId aktoerId) {
        Optional<String> navKontorForArbeidsliste = hentNavKontorForArbeidsliste(aktoerId);

        if (navKontorForArbeidsliste.isEmpty()) {
            log.info("Bruker {} har ikke NAV-kontor på arbeidsliste", aktoerId.toString());
            return false;
        }

        final Optional<String> navKontorForBruker = brukerService.hentNavKontor(aktoerId);
        if (navKontorForBruker.isEmpty()) {
            log.error("Kunne ikke hente NAV-kontor fra db-link til arena for bruker {}", aktoerId.toString());
            return false;
        }

        log.info("Bruker {} er på kontor {} mens arbeidslisten er lagret på {}", aktoerId.toString(), navKontorForBruker.get(), navKontorForArbeidsliste.get());
        return !navKontorForBruker.orElseThrow().equals(navKontorForArbeidsliste.orElseThrow());
    }
}
