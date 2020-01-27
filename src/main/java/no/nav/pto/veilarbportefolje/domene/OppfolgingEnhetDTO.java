package no.nav.pto.veilarbportefolje.domene;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OppfolgingEnhetDTO {
    String fnr;
    String aktorId;
    String enhetId;
    String personId;
}
