package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Sikkerhetstiltak {
    public final String tiltakstype;
    public final String beskrivelse;
    public final LocalDate gyldigFra;
    public final LocalDate gyldigTil;
}
