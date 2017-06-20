package no.nav.fo.service;

import javaslang.control.Try;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
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

    public Try<Arbeidsliste> getArbeidsliste(ArbeidslisteData data) {
        return arbeidslisteRepository
                .retrieveArbeidsliste(hentAktoerId(data.getFnr()))
                .map(this::setOppfolgendeVeileder)
                .map(Arbeidsliste::of);
    }

    public Try<Boolean> createArbeidsliste(ArbeidslisteData data) {
        data.setAktoerId(hentAktoerId(data.getFnr()));
        return arbeidslisteRepository.insertArbeidsliste(data);
    }

    public Try<Integer> updateArbeidsliste(ArbeidslisteData data) {
        return arbeidslisteRepository.updateArbeidsliste(data.setAktoerId(hentAktoerId(data.getFnr())));
    }

    public Try<Integer> deleteArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository.deleteArbeidsliste(hentAktoerId(fnr));
    }

    public String hentEnhet(Fnr fnr) {
        return brukerRepository
                .retrieveEnhet(fnr)
                .getOrElseThrow(x -> new RestBadGateWayException("Kunne ikke hente enhet for denne brukeren"));
    }

    private AktoerId hentAktoerId(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr.toString())
                .map(AktoerId::new)
                .orElseThrow(() -> new RestBadGateWayException("Fant ikke aktoerId for gitt fnr"));
    }

    private ArbeidslisteData setOppfolgendeVeileder(ArbeidslisteData data) {
        return brukerRepository
                .retrieveVeileder(data.getAktoerId())
                .map(x -> x.equals(data.getVeilederId()))
                .map(data::setIsOppfolgendeVeileder)
                .getOrElseThrow(() -> new RestNotFoundException("Fant ikke nåværende veileder for bruker"));
    }

    public Boolean erVeilederForBruker(Fnr fnr, String veilederId) {
        AktoerId aktoerId = hentAktoerId(fnr);
        return brukerRepository
                .retrieveVeileder(aktoerId)
                .map(currentVeileder -> currentVeileder.equals(veilederId))
                .getOrElseThrow(() -> new RestNotFoundException("Fant ikke nåværende veileder for bruker"));
    }
}
