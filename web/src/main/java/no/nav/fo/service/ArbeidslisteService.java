package no.nav.fo.service;

import javaslang.control.Try;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.exception.RestBadGateWayException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;

import javax.inject.Inject;

public class ArbeidslisteService {
    @Inject
    private AktoerService aktoerService;

    @Inject
    private ArbeidslisteRepository arbeidslisteRepository;

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository.retrieveArbeidsliste(hentAktoerId(fnr));
    }

    public Try<Boolean> createArbeidsliste(ArbeidslisteData data) {
        String aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository
                .insertArbeidsliste(data.setAktoerID(aktoerId));
    }

    public Try<Integer> updateArbeidsliste(ArbeidslisteData data) {
        String aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerID(aktoerId));
    }

    public Try<Integer> deleteArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository.deleteArbeidsliste(hentAktoerId(fnr));
    }

    private String hentAktoerId(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr.toString())
                .orElseThrow(() -> new RestBadGateWayException("Fant ikke aktoerId ved kall mot aktoerservice"));
    }
}
