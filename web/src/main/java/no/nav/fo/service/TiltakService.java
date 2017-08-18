package no.nav.fo.service;


import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.EnhetTiltakRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.EnhetTiltak;
import no.nav.fo.domene.aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.util.AktivitetUtils;
import no.nav.fo.util.BatchConsumer;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

import static no.nav.fo.util.AktivitetUtils.hentAktivitetBrukerOppdatering;
import static no.nav.fo.util.BatchConsumer.batchConsumer;

@Slf4j
public class TiltakService {

    @Inject
    private EnhetTiltakRepository enhetTiltakRepository;

    public Try<EnhetTiltak> hentEnhettiltak(String enhet) {
        return enhetTiltakRepository.retrieveEnhettiltak(enhet);
    }
}
