package no.nav.fo.service;

import javaslang.control.Try;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
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

    @Inject
    private BrukerRepository brukerRepository;

    public Try<Arbeidsliste> getArbeidsliste(Fnr fnr) {

        String currentVeileder = brukerRepository
                .retrieveVeileder(hentAktoerId(fnr))
                .getOrElse("");

        return arbeidslisteRepository
                        .retrieveArbeidsliste(hentAktoerId(fnr))
                        .map(arbeidsliste -> setIsOppfolgendeVeileder(currentVeileder, arbeidsliste));
    }

    private Arbeidsliste setIsOppfolgendeVeileder(String currentVeileder, Arbeidsliste arbeidsliste) {
        return arbeidsliste.setVeilederOppfolgendeVeileder(
                arbeidsliste.getVeilederId().equals(currentVeileder)
        );
    }

    public Try<Boolean> createArbeidsliste(ArbeidslisteData data) {
        AktoerId aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository.insertArbeidsliste(data.setAktoerId(aktoerId));
    }

    public Try<Integer> updateArbeidsliste(ArbeidslisteData data) {
        AktoerId aktoerId = hentAktoerId(data.getFnr());
        return arbeidslisteRepository
                .updateArbeidsliste(data.setAktoerId(aktoerId));
    }

    public Try<Integer> deleteArbeidsliste(Fnr fnr) {
        return arbeidslisteRepository.deleteArbeidsliste(hentAktoerId(fnr));
    }

    private AktoerId hentAktoerId(Fnr fnr) {
        return aktoerService
                .hentAktoeridFraFnr(fnr.toString())
                .map(AktoerId::new)
                .orElseThrow(() -> new RestBadGateWayException("Fant ikke aktoerId ved kall mot aktoerservice"));
    }
}
