package no.nav.pto.veilarbportefolje.sisteendring;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class SisteEndringDTO {
    private AktoerId aktoerId;
    private SisteEndringsKategorier kategori;
    private Timestamp tidspunkt;

}