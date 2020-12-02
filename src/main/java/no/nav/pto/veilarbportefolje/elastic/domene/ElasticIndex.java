package no.nav.pto.veilarbportefolje.elastic.domene;

import lombok.Value;

@Value
public class ElasticIndex {
    String index;

    public static ElasticIndex of(String index) {
        return new ElasticIndex(index);
    }
}
