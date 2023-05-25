package no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class BarnUnder18AarData {
    Integer alder;
    String diskresjonskode;

    public BarnUnder18AarData(Integer alder, String s) {
        this.alder = alder;
        this.diskresjonskode = s;
    }
}
