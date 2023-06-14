package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class BarnUnder18Aar {
    Fnr fnr;
    LocalDate fodselsdato;
    String diskresjonskode;

}
