package no.nav.fo.service;


import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.EnhetTiltakRepository;
import no.nav.fo.domene.EnhetTiltak;

import javax.inject.Inject;

@Slf4j
public class TiltakService {

    @Inject
    private EnhetTiltakRepository enhetTiltakRepository;

    public Try<EnhetTiltak> hentEnhettiltak(String enhet) {
        return enhetTiltakRepository.retrieveEnhettiltak(enhet);
    }
}
