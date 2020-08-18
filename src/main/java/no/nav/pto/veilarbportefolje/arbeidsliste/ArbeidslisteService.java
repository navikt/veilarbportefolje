package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import io.vavr.control.Validation;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.pto.veilarbportefolje.util.ValideringsRegler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.lang.String.format;

@Service
public class ArbeidslisteService {
    private final AktorregisterClient aktorregisterClient;
    private final ArbeidslisteRepository arbeidslisteRepository;
    private final BrukerRepository brukerRepository;
    private final ElasticIndexer elasticIndexer;
    private final MetricsClient metricsClient;

    @Autowired
    public ArbeidslisteService(
            AktorregisterClient aktorregisterClient,
            ArbeidslisteRepository arbeidslisteRepository,
            BrukerRepository brukerRepository,
            ElasticIndexer elasticIndexer,
            MetricsClient metricsClient) {
        this.aktorregisterClient = aktorregisterClient;
        this.arbeidslisteRepository = arbeidslisteRepository;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.metricsClient = metricsClient;

    }


    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return hentAktoerId(fnr).map(this::getArbeidsliste).get();
    }

    public Try<Arbeidsliste> getArbeidsliste(AktoerId aktoerId) {
        return arbeidslisteRepository.retrieveArbeidsliste(aktoerId);
    }

    public Try<AktoerId> createArbeidsliste(ArbeidslisteDTO data) {

        metricsClient.report((new Event("arbeidsliste.opprettet")));

        Try<AktoerId> aktoerId = hentAktoerId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }

        data.setAktoerId(aktoerId.get());
        return arbeidslisteRepository
                .insertArbeidsliste(data)
                .onSuccess(elasticIndexer::indekser);
    }

    public Try<AktoerId> updateArbeidsliste(ArbeidslisteDTO data) {
        Try<AktoerId> aktoerId = hentAktoerId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }

        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerId(aktoerId.get()))
                .onSuccess(elasticIndexer::indekser);
    }

    public Try<AktoerId> deleteArbeidsliste(Fnr fnr) {
        Try<AktoerId> aktoerId = hentAktoerId(fnr);
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        return arbeidslisteRepository
                .deleteArbeidsliste(aktoerId.get())
                .onSuccess(elasticIndexer::indekser);
    }

    public Try<String> hentEnhet(Fnr fnr) {
        return brukerRepository.retrieveEnhet(fnr);
    }

    public Result<Integer> deleteArbeidslisteForAktoerId(AktoerId aktoerId) {
        return arbeidslisteRepository.deleteArbeidslisteForAktoerid(aktoerId);
    }

    private Try<AktoerId> hentAktoerId(Fnr fnr) {
        return Try.of(() -> AktoerId.of(aktorregisterClient.hentAktorId(fnr.toString())));
    }

    public Validation<String, List<Fnr>> erVeilederForBrukere(List<Fnr> fnrs) {
        List<Fnr> validerteFnrs = new ArrayList<>(fnrs.size());
        fnrs.forEach(fnr -> {
            if(erVeilederForBruker(fnr.toString()).isValid()) {
                validerteFnrs.add(fnr);
            }
        });

        return validerteFnrs.size() == fnrs.size() ? valid(validerteFnrs) : invalid(format("Veileder har ikke tilgang til alle brukerene i listen: %s", fnrs));

    }

    public Validation<String, Fnr> erVeilederForBruker(String fnr) {
        VeilederId veilederId = AuthUtils.getInnloggetVeilederIdent();

        Boolean erVeilederForBruker =
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
        return brukerRepository.retrieveVeileder(aktoerId).
                map(currentVeileder -> currentVeileder.equals(veilederId)).getOrElse(false);
    }
}
