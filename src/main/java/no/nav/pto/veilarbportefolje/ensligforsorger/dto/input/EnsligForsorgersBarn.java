package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class EnsligForsorgersBarn {
    String personIdent;
    LocalDate fødselTermindato;
}
