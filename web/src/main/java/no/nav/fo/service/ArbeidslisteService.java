package no.nav.fo.service;

import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.exception.RestBadGateWayException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;

import javax.inject.Inject;
import java.util.Optional;

public class ArbeidslisteService {
    @Inject
    private AktoerService aktoerService;

    @Inject
    private ArbeidslisteRepository arbeidslisteRepository;

    public Optional<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository.retrieveArbeidsliste(hentAktoerId(fnr));
    }

    public Optional<ArbeidslisteData> createArbeidsliste(ArbeidslisteData data) {
        String aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository
                .insertArbeidsliste(data.setAktoerID(aktoerId));
    }

    public Optional<ArbeidslisteData> updateArbeidsliste(ArbeidslisteData data) {
        String aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerID(aktoerId));
    }

    public Optional<String> deleteArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository.deleteArbeidsliste(hentAktoerId(fnr));
    }

    private String hentAktoerId(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr.toString())
                .orElseThrow(() -> new RestBadGateWayException("Fant ikke aktoerId ved kall mot aktoerservice"));
    }
}
