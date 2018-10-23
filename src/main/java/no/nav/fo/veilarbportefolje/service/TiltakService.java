package no.nav.fo.veilarbportefolje.service;


import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.database.EnhetTiltakRepository;
import no.nav.fo.veilarbportefolje.domene.EnhetTiltak;

import javax.inject.Inject;

@Slf4j
public class TiltakService {

    @Inject
    private EnhetTiltakRepository enhetTiltakRepository;

    public Try<EnhetTiltak> hentEnhettiltak(String enhet) {
        return enhetTiltakRepository.retrieveEnhettiltak(enhet);
    }
}
