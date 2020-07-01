package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArbeidslisteService {
    private AktoerService aktoerService;
    private ArbeidslisteRepository arbeidslisteRepository;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;
    private MetricsClient metricsClient;

    @Autowired
    public ArbeidslisteService(
            AktoerService aktoerService,
            ArbeidslisteRepository arbeidslisteRepository,
            BrukerRepository brukerRepository,
            ElasticIndexer elasticIndexer,
            MetricsClient metricsClient) {
        this.aktoerService = aktoerService;
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
        return aktoerService
                .hentAktoeridFraFnr(fnr);
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
