package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;

@Data
public class BarnUnder18AarData {
    Integer alder;
    String diskresjonskode;

    public BarnUnder18AarData(Integer alder, String s) {
        this.alder = alder;
        this.diskresjonskode = s;
    }
}
