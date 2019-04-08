package no.nav.fo.veilarbportefolje.indeksering.domene;

import lombok.Data;

@Data
public class Bucket {
    String key;
    Long doc_count;
}
