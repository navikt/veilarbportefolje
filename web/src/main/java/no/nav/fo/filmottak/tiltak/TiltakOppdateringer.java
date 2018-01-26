package no.nav.fo.filmottak.tiltak;

import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;

@Value
@Builder(toBuilder = true)
class TiltakOppdateringer {
    private Timestamp aktivitetStart;
    private Timestamp nesteAktivitetStart;
    private Timestamp forrigeAktivitetStart;
    private Timestamp nyesteUtlopteAktivitet;

}
