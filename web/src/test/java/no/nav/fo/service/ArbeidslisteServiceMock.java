package no.nav.fo.service;

import no.nav.fo.domene.Arbeidsliste;

import java.sql.Timestamp;
import java.util.Optional;

public class ArbeidslisteServiceMock implements ArbeidslisteService {
    @Override
    public Optional<Arbeidsliste> getArbeidsliste(String fnr) {
        Arbeidsliste arbeidsliste = new Arbeidsliste()
                .setArbeidsliste(true)
                .setKommentar("Dette er en kommentar av en arbeidsliste")
                .setFrist(Timestamp.valueOf("2017-10-11"))
                .setEndringstidspunkt(Timestamp.valueOf("2017-10-11"))
                .setVeilederId("X11111")
                .setVeilederOppfolgendeVeileder(true);

        return Optional.of(arbeidsliste);
    }
}
