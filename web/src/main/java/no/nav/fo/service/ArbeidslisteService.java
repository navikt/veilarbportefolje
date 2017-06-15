package no.nav.fo.service;

import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.Fnr;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteUpdate;

import javax.inject.Inject;
import java.util.Optional;

public class ArbeidslisteService {
    @Inject
    private AktoerService aktoerService;

    @Inject
    private ArbeidslisteRepository arbeidslisteRepository;

    public Optional<Arbeidsliste> getArbeidsliste(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr.toString())
                .flatMap(aktoerId -> arbeidslisteRepository.retrieveArbeidsliste(aktoerId));
    }

    public Optional<ArbeidslisteUpdate> createArbeidsliste(ArbeidslisteUpdate data) {
        return aktoerService
                .hentAktoeridFraFnr(data.getFnr().toString())
                .map(data::setAktoerID)
                .flatMap(aktoerId -> arbeidslisteRepository.insertArbeidsliste(data));
    }

    public Optional<ArbeidslisteUpdate> updateArbeidsliste(ArbeidslisteUpdate data) {
        return aktoerService
                .hentAktoeridFraFnr(data.getFnr().toString())
                .map(data::setAktoerID)
                .flatMap(aktoerId -> arbeidslisteRepository.updateArbeidsliste(data));
    }

    public Optional<String> deleteArbeidsliste(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr.toString())
                .flatMap(aktoerId -> arbeidslisteRepository.deleteArbeidsliste(aktoerId));
    }
}
