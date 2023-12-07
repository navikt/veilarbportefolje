package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.RequiredArgsConstructor;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Service
@RequiredArgsConstructor
public class ArbeidslisteService {
    private static final Logger log = LoggerFactory.getLogger(ArbeidslisteService.class);

    private final AktorClient aktorClient;
    private final ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;
    private final BrukerServiceV2 brukerServiceV2;
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final MetricsClient metricsClient;

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return arbeidslisteRepositoryV2.retrieveArbeidsliste(fnr);
    }

    public List<Arbeidsliste> getArbeidslisteForVeilederPaEnhet(EnhetId enhet, VeilederId veilederident) {
        return arbeidslisteRepositoryV2.hentArbeidslisteForVeilederPaEnhet(enhet, veilederident);
    }

    public Try<ArbeidslisteDTO> createArbeidsliste(ArbeidslisteDTO dto) {

        metricsClient.report((new Event("arbeidsliste.opprettet")));

        Try<AktorId> aktoerId = hentAktorId(dto.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        dto.setAktorId(aktoerId.get());

        NavKontor navKontorForBruker = brukerServiceV2.hentNavKontor(dto.getFnr()).orElseThrow();
        dto.setNavKontorForArbeidsliste(navKontorForBruker.getValue());
        return arbeidslisteRepositoryV2.insertArbeidsliste(dto)
                .onSuccess(opensearchIndexerV2::updateArbeidsliste);
    }

    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        Try<AktorId> aktoerId = hentAktorId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        data.setAktorId(aktoerId.get());
        return arbeidslisteRepositoryV2.updateArbeidsliste(data)
                .onSuccess(opensearchIndexerV2::updateArbeidsliste);
    }

    public int slettArbeidsliste(AktorId aktoerId) {
        final int rowsUpdated = arbeidslisteRepositoryV2.slettArbeidsliste(aktoerId);
        if (rowsUpdated == 1) {
            opensearchIndexerV2.slettArbeidsliste(aktoerId);
        }
        return rowsUpdated;
    }

    public int slettArbeidsliste(Fnr fnr) {
        Optional<AktorId> aktoerId = brukerServiceV2.hentAktorId(fnr);
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
        return brukerServiceV2
                .hentVeilederForBruker(aktoerId)
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .orElse(false);
    }

    public boolean brukerHarByttetNavKontor(AktorId aktoerId) {
        Optional<String> navKontorForArbeidsliste = arbeidslisteRepositoryV2.hentNavKontorForArbeidsliste(aktoerId);

        if (navKontorForArbeidsliste.isEmpty()) {
            secureLog.info("Bruker {} har ikke NAV-kontor på arbeidsliste", aktoerId.toString());
            return false;
        }

        final Optional<String> navKontorForBruker = brukerServiceV2.hentNavKontor(aktoerId).map(NavKontor::getValue);
        if (navKontorForBruker.isEmpty()) {
            secureLog.error("Kunne ikke hente NAV-kontor fra db-link til arena for bruker {}", aktoerId.toString());
            return false;
        }

        boolean navkontorForBrukerUlikNavkontorForArbeidsliste = !navKontorForBruker.orElseThrow().equals(navKontorForArbeidsliste.orElseThrow());

        if (navkontorForBrukerUlikNavkontorForArbeidsliste) {
            secureLog.info("Bruker {} er på kontor {} mens arbeidslisten er lagret på et annet kontor {}", aktoerId.toString(), navKontorForBruker.get(), navKontorForArbeidsliste.get());
        } else {
            secureLog.info("Bruker {} er på kontor {} og arbeidslisten er lagret på samme kontor {}", aktoerId.toString(), navKontorForBruker.get(), navKontorForArbeidsliste.get());
        }

        return navkontorForBrukerUlikNavkontorForArbeidsliste;
    }
}
