package no.nav.fo.veilarbportefolje.indeksering.domene;

import lombok.Data;

import java.util.List;

@Data
public class Hits {
    int total;
    List<Hit> hits;

}
