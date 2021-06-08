package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class UtdanningsAktivitet extends GoldenGateDTO {
    UtdanningsAktivitetInnhold before;
    UtdanningsAktivitetInnhold after;
}