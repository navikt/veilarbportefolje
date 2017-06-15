package no.nav.fo.service;

import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteUpdate;

import javax.inject.Inject;
import java.util.Optional;

public class ArbeidslisteService {
    @Inject
    private AktoerService aktoerService;

    @Inject
    private ArbeidslisteRepository arbeidslisteRepository;

    public Optional<Arbeidsliste> getArbeidsliste(String fnr) {
        return arbeidslisteRepository.retrieveArbeidsliste(fnr);
    }

    public Optional<ArbeidslisteUpdate> createArbeidsliste(ArbeidslisteUpdate data) {
        return aktoerService
                .hentAktoeridFraFnr(data.getFnr().toString())
                .map(data::setAktoerID)
                .flatMap(x -> arbeidslisteRepository.insertArbeidsliste(data));
    }

    public Optional<ArbeidslisteUpdate> updateArbeidsliste(ArbeidslisteUpdate data) {
        return Optional.empty();
    }

    public Optional<Arbeidsliste> deleteArbeidsliste(String fnr) {
        return Optional.empty();
    }

}
