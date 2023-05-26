package no.nav.pto.veilarbportefolje.persononinfo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class Familiemedlem {
    LocalDate fodselsdato;
    String gradering;       //diskresjonskode
    RelasjonsBosted relasjonsBosted;
}