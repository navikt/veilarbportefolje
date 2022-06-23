package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Statsborgerskap {
    public final String statsborgerskap;
    public final LocalDate gyldigFra;
    public final LocalDate gyldigTil;
}
