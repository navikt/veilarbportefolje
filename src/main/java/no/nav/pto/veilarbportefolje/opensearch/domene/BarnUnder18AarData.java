package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;

@Data
public class BarnUnder18AarData {
    Long alder;
    Boolean bor_med_foresatt;
    String diskresjonskode;

    public BarnUnder18AarData(long l, boolean b, String s) {
        this.alder = l;
        this.bor_med_foresatt = b;
        this.diskresjonskode = s;
    }
}
