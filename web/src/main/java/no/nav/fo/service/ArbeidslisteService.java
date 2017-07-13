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
import no.nav.fo.exception.RestTilgangException;
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

    public Try<Arbeidsliste> getArbeidsliste(ArbeidslisteData data) {
        AktoerId aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository
                .retrieveArbeidsliste(aktoerId)
                .map(arbeidsliste -> this.setOppfolgendeVeileder(arbeidsliste, aktoerId));
    }

    public Try<AktoerId> createArbeidsliste(ArbeidslisteData data) {
        AktoerId aktoerId = hentAktoerId(data.getFnr());
        data.setAktoerId(aktoerId);
        return arbeidslisteRepository
                .insertArbeidsliste(data)
                .onSuccess(solrService::indekserBrukerdata);
    }

    public Try<AktoerId> updateArbeidsliste(ArbeidslisteData data) {
        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerId(hentAktoerId(data.getFnr())))
                .onSuccess(solrService::indekserBrukerdata);
    }

    public Try<AktoerId> deleteArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository
                .deleteArbeidsliste(hentAktoerId(fnr))
                .onSuccess(solrService::indekserBrukerdata);
    }

    public String hentEnhet(Fnr fnr) {
        return brukerRepository
                .retrieveEnhet(fnr)
                .getOrElseThrow(x -> new RestBadGateWayException("Kunne ikke hente enhet for denne brukeren"));
    }

    private AktoerId hentAktoerId(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr)
                .orElseThrow(() -> new RestBadGateWayException("Fant ikke aktoerId for gitt fnr"));
    }

    private Arbeidsliste setOppfolgendeVeileder(Arbeidsliste arbeidsliste, AktoerId aktoerId) {
        return brukerRepository
                .retrieveVeileder(aktoerId)
                .map(veilederId -> veilederId.equals(arbeidsliste.getSistEndretAv()))
                .map(arbeidsliste::setIsOppfolgendeVeileder)
                .getOrElseThrow(() -> new RestNotFoundException("Fant ikke nåværende veileder for bruker"));
    }

    public Boolean erVeilederForBruker(Fnr fnr, VeilederId veilederId) {
        AktoerId aktoerId = hentAktoerId(fnr);

        return brukerRepository
                .retrieveVeileder(aktoerId)
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .getOrElseThrow(() -> new RestTilgangException("Fant ikke nåværende veileder for bruker"));
    }

}
