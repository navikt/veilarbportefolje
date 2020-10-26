package no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr202.tiltak;

import lombok.Value;

import java.sql.Timestamp;
import java.util.Optional;

@Value(staticConstructor = "of")
class TiltakDatoer {
    private Optional<Timestamp> startDato;
    private Optional<Timestamp> sluttDato;
}
