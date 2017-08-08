package no.nav.fo.service;

import io.vavr.control.Try;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.exception.RestBadGateWayException;
import no.nav.fo.exception.RestNotFoundException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;

import javax.inject.Inject;

public class ArbeidslisteService {

    @Inject
    private AktoerService aktoerService;

    @Inject
    private ArbeidslisteRepository arbeidslisteRepository;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private SolrService solrService;

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        Try<AktoerId> aktoerId = hentAktoerId(fnr);
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }

        return arbeidslisteRepository
                .retrieveArbeidsliste(aktoerId.get())
                .map(arbeidsliste -> this.setOppfolgendeVeileder(arbeidsliste, aktoerId.get()));
    }

    public Try<Arbeidsliste> getArbeidsliste(AktoerId aktoerId) {

        return arbeidslisteRepository
                .retrieveArbeidsliste(aktoerId)
                .map(arbeidsliste -> this.setOppfolgendeVeileder(arbeidsliste, aktoerId));
    }

    public Try<AktoerId> createArbeidsliste(ArbeidslisteData data) {

        Try<AktoerId> aktoerId = hentAktoerId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }

        data.setAktoerId(aktoerId.get());
        return arbeidslisteRepository
                .insertArbeidsliste(data)
                .onSuccess(solrService::indekserBrukerdata);
    }

    public Try<AktoerId> updateArbeidsliste(ArbeidslisteData data) {
        Try<AktoerId> aktoerId = hentAktoerId(data.getFnr());
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }

        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerId(aktoerId.get()))
                .onSuccess(solrService::indekserBrukerdata);
    }

    public Try<AktoerId> deleteArbeidsliste(Fnr fnr) {
        Try<AktoerId> aktoerId = hentAktoerId(fnr);
        if (aktoerId.isFailure()) {
            return Try.failure(aktoerId.getCause());
        }
        return arbeidslisteRepository
                .deleteArbeidsliste(aktoerId.get())
                .onSuccess(solrService::indekserBrukerdata);
    }

    public Try<AktoerId> deleteArbeidsliste(AktoerId aktoerId) {
        return arbeidslisteRepository
                .deleteArbeidsliste(aktoerId)
                .onSuccess(solrService::indekserBrukerdata);
    }

    public String hentEnhet(Fnr fnr) {
        return brukerRepository
                .retrieveEnhet(fnr)
                .getOrElseThrow(x -> new RestBadGateWayException("Kunne ikke hente enhet for denne brukeren"));
    }

    private Try<AktoerId> hentAktoerId(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr);
    }

    private Arbeidsliste setOppfolgendeVeileder(Arbeidsliste arbeidsliste, AktoerId aktoerId) {
        return brukerRepository
                .retrieveVeileder(aktoerId)
                .map(veilederId -> veilederId.equals(arbeidsliste.getSistEndretAv()))
                .map(arbeidsliste::setIsOppfolgendeVeileder)
                .getOrElseThrow(() -> new RestNotFoundException("Fant ikke nåværende veileder for bruker"));
    }

    public Boolean erVeilederForBruker(Fnr fnr, VeilederId veilederId) {
        return
                hentAktoerId(fnr)
                        .flatMap(brukerRepository::retrieveVeileder)
                        .map(currentVeileder -> currentVeileder.equals(veilederId))
                        .getOrElse(false);
    }
}
