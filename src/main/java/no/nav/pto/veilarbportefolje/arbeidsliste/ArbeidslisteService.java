package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArbeidslisteService {
    private static final Logger log = LoggerFactory.getLogger(ArbeidslisteService.class);

    private final AktorClient aktorClient;
    private final ArbeidslisteRepositoryV1 arbeidslisteRepositoryOracle;
    private final ArbeidslisteRepositoryV2 arbeidslisteRepositoryPostgres;
    private final BrukerService brukerService;
    private final ElasticServiceV2 elasticServiceV2;
    private final UnleashService unleashService;
    private final MetricsClient metricsClient;

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr, String innloggetVeileder) {
        return hentAktorId(fnr).map((aktoerId) -> getArbeidsliste(aktoerId, innloggetVeileder)).get();
    }

    public Try<Arbeidsliste> getArbeidsliste(AktorId aktoerId, String innloggetVeileder) {
        if (erPostgresPa(unleashService, innloggetVeileder)) {
            return arbeidslisteRepositoryPostgres.retrieveArbeidsliste(aktoerId);
        }
        return arbeidslisteRepositoryOracle.retrieveArbeidsliste(aktoerId);
    }

    public Try<ArbeidslisteDTO> createArbeidsliste(ArbeidslisteDTO dto) {

        metricsClient.report((new Event("arbeidsliste.opprettet")));

        Try<AktorId> aktoerId = hentAktorId(dto.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        dto.setAktorId(aktoerId.get());

        String navKontorForBruker = brukerService.hentNavKontorFraDbLinkTilArena(dto.getFnr()).orElseThrow();
        dto.setNavKontorForArbeidsliste(navKontorForBruker);
        arbeidslisteRepositoryPostgres.insertArbeidsliste(dto);
        return arbeidslisteRepositoryOracle.insertArbeidsliste(dto)
                .onSuccess(elasticServiceV2::updateArbeidsliste);
    }

    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        Try<AktorId> aktoerId = hentAktorId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        data.setAktorId(aktoerId.get());
        arbeidslisteRepositoryPostgres.updateArbeidsliste(data);
        return arbeidslisteRepositoryOracle
                .updateArbeidsliste(data)
                .onSuccess(elasticServiceV2::updateArbeidsliste);
    }

    public int slettArbeidsliste(AktorId aktoerId) {
        arbeidslisteRepositoryPostgres.slettArbeidsliste(aktoerId);
        final int rowsUpdated = arbeidslisteRepositoryOracle.slettArbeidsliste(aktoerId);
        if (rowsUpdated == 1) {
            elasticServiceV2.slettArbeidsliste(aktoerId);
        }
        return rowsUpdated;
    }

    public int slettArbeidsliste(Fnr fnr) {
        Optional<AktorId> aktoerId = brukerService.hentAktorId(fnr);
        if (aktoerId.isPresent()) {
            return slettArbeidsliste(aktoerId.get());
        }
        log.error("fant ikke aktørId på fnr");
        return -1;
    }

    private Try<AktorId> hentAktorId(Fnr fnr) {
        return Try.of(() -> aktorClient.hentAktorId(fnr));
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
            return valid(Fnr.ofValidFnr(fnr));
        }
        return invalid(format("Veileder %s er ikke veileder for bruker med fnr %s", veilederId, fnr));
    }


    public Boolean erVeilederForBruker(Fnr fnr, VeilederId veilederId) {
        return hentAktorId(fnr)
                .map(aktoerId -> erVeilederForBruker(aktoerId, veilederId))
                .getOrElse(false);
    }

    public Boolean erVeilederForBruker(AktorId aktoerId, VeilederId veilederId) {
        return brukerService
                .hentVeilederForBruker(aktoerId)
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .orElse(false);
    }

    public boolean brukerHarByttetNavKontorOracle(AktorId aktoerId) {
        Optional<String> navKontorForArbeidsliste = arbeidslisteRepositoryOracle.hentNavKontorForArbeidsliste(aktoerId);

        if (navKontorForArbeidsliste.isEmpty()) {
            log.info("Bruker {} har ikke NAV-kontor på arbeidsliste", aktoerId);
            return false;
        }

        final Optional<String> navKontorForBruker = brukerService.hentNavKontor(aktoerId);
        if (navKontorForBruker.isEmpty()) {
            log.error("Kunne ikke hente NAV-kontor fra db-link til arena for bruker {}", aktoerId);
            return false;
        }

        log.info("Bruker {} er på kontor {} mens arbeidslisten er lagret på {}", aktoerId, navKontorForBruker.get(), navKontorForArbeidsliste.get());
        return !navKontorForBruker.orElseThrow().equals(navKontorForArbeidsliste.orElseThrow());
    }

    public boolean brukerHarByttetNavKontorPostgres(AktorId aktoerId) {
        Optional<String> navKontorForArbeidsliste = arbeidslisteRepositoryPostgres.hentNavKontorForArbeidsliste(aktoerId);

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

    //TODO: Slett ved full overgang til postgres
    public void slettArbeidslistePostgres(AktorId aktoerId) {
        arbeidslisteRepositoryPostgres.slettArbeidsliste(aktoerId);
    }
}
