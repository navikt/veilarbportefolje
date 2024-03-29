package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;

import java.util.List;

@Data
public class Hits {
    HitsTotal total;
    List<Hit> hits;

    @Data
    public static class HitsTotal {
        int value;
    }
}
