package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Oppfolgingstatus {
    private String servicegruppekode;
    private String formidlingsgruppekode;
    private String veileder;
    private boolean oppfolgingsbruker;
}
