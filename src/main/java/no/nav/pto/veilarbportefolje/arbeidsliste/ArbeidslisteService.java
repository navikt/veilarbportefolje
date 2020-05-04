package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepository;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.service.AktoerService;

import javax.inject.Inject;

import static no.nav.metrics.MetricsFactory.createEvent;

public class ArbeidslisteService {

    @Inject
    private AktoerService aktoerService;

    @Inject
    private ArbeidslisteRepository arbeidslisteRepository;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private ElasticIndexer elasticIndexer;

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return hentAktoerId(fnr).map(this::getArbeidsliste).get();
    }

    public Try<Arbeidsliste> getArbeidsliste(AktoerId aktoerId) {
        return arbeidslisteRepository.retrieveArbeidsliste(aktoerId);
    }

    public Try<AktoerId> createArbeidsliste(ArbeidslisteDTO data) {

        createEvent("arbeidsliste.opprettet").report();

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

    public void deleteArbeidslisteForAktoerId(AktoerId aktoerId) {
        arbeidslisteRepository.deleteArbeidslisteForAktoerid(aktoerId);
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
