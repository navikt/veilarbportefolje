package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ArenaAktivitetDTO {
    private String aktoerid;
    private String aktivitetId;
}
