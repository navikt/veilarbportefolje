package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;

@Data
public class BarnUnder18AarData {
    Long alder;
    String diskresjonskode;

    public BarnUnder18AarData(long l, String s) {
        this.alder = l;
        this.diskresjonskode = s;
    }
}
