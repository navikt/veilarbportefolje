package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;

@Slf4j
@Service
public class ArbeidslisteService {
    private final AktorregisterClient aktorregisterClient;
    private final ArbeidslisteRepository arbeidslisteRepository;
    private final BrukerService brukerService;
    private final ElasticServiceV2 elasticelasticService;
    private final MetricsClient metricsClient;

    @Autowired
    public ArbeidslisteService(
            AktorregisterClient aktorregisterClient,
            ArbeidslisteRepository arbeidslisteRepository,
            BrukerService brukerService,
            ElasticServiceV2 elasticelasticService,
            MetricsClient metricsClient
    ) {
        this.aktorregisterClient = aktorregisterClient;
        this.arbeidslisteRepository = arbeidslisteRepository;
        this.brukerService = brukerService;
        this.elasticelasticService = elasticelasticService;
        this.metricsClient = metricsClient;
    }

    public Optional<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return elasticelasticService.hentArbeidsListe(fnr);
    }

    public Try<Arbeidsliste> getArbeidsliste(AktoerId aktoerId) {
        List<Arbeidsliste> arbeidslistes = elasticelasticService.hentArbeidsListe(aktoerId);
        if(arbeidslistes.size() == 1){
            return Try.success(arbeidslistes.get(0));
        }else if(arbeidslistes.size() > 1){
            log.warn("Flere arbeidslister lagret på samme aktørId: " +aktoerId);
            return Try.success(arbeidslistes.get(0));
        }else{
            return Try.failure(new NoSuchElementException("Fant ingen arbeids liste på aktoerId: " +aktoerId));
        }
    }

    public Try<ArbeidslisteDTO> createArbeidsliste(ArbeidslisteDTO dto) {
        metricsClient.report((new Event("arbeidsliste.opprettet")));

        Optional<AktoerId> aktoerId = hentAktoerId(dto.getFnr());
        if (aktoerId.isEmpty()) {
            return Try.failure(new NoSuchElementException("Fant ingen bruker med gitt fnr"));
        }
        dto.setAktoerId(aktoerId.get());

        String navKontorForBruker = brukerService.hentNavKontorFraDbLinkTilArena(dto.getFnr()).orElseThrow();
        dto.setNavKontorForArbeidsliste(navKontorForBruker);

        return arbeidslisteRepository.insertArbeidsliste(dto).onSuccess(elasticelasticService::updateArbeidsliste);
    }

    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
        Optional<AktoerId> aktoerId = hentAktoerId(data.getFnr());
        if (aktoerId.isEmpty()) {
            return Try.failure(new NoSuchElementException("Fant ingen bruker med gitt fnr"));
        }

        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerId(aktoerId.get()))
                .onSuccess(elasticelasticService::updateArbeidsliste);
    }

    public Try<AktoerId> deleteArbeidsliste(Fnr fnr) {
        Optional<AktoerId> aktoerId = hentAktoerId(fnr);
        if (aktoerId.isEmpty()) {
            return Try.failure(new NoSuchElementException("Fant ingen bruker med gitt fnr"));
        }
        return arbeidslisteRepository
                .deleteArbeidsliste(aktoerId.get())
                .onSuccess(a -> elasticelasticService.slettArbeidsliste(fnr, a));
    }

    //TODO: fix
    public Integer deleteArbeidslisteForAktoerId(AktoerId aktoerId) {
        return arbeidslisteRepository.deleteArbeidslisteForAktoerid(aktoerId);
    }

    private Optional<AktoerId> hentAktoerId(Fnr fnr) {
        Optional<AktoerId> aktoerId = elasticelasticService.hentAktoerId(fnr);
        if(aktoerId.isPresent()){
            return aktoerId;
        }else{
            try {
                AktoerId aktoerIdFraAreana = AktoerId.of(aktorregisterClient.hentAktorId(fnr.toString()));
                return Optional.of(aktoerIdFraAreana);
            }catch (Exception e){
                return Optional.empty();
            }
        }
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
                .isPresent();
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
}
