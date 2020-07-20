package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class AktoerAktiviteter {
    private final String aktoerid;
    private List<AktivitetDTO> aktiviteter;
}
