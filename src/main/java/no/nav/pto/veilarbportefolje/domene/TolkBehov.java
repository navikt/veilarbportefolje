package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TolkBehov {
    public final String talespraaktolk;
    public final String tegnspraaktolk;
    public final LocalDate sistOppdatert;
}
