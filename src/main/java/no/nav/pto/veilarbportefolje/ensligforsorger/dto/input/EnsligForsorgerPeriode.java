package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class EnsligForsorgerPeriode {
    LocalDate stønadFraOgMed;
    LocalDate stønadTilOgMed;
    Periodetype periodetype;
    Aktivitetstype aktivitet;
    List<Barn> barn;
    String behandlingId;
    Boolean harAktivitetsplikt;
}
