package no.nav.fo.veilarbportefolje.filmottak.tiltak;

import lombok.Value;

import java.sql.Timestamp;
import java.util.Optional;

@Value(staticConstructor = "of")
class TiltakDatoer {
    private Optional<Timestamp> startDato;
    private Optional<Timestamp> sluttDato;
}
