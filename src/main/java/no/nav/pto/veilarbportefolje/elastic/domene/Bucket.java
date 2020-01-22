package no.nav.pto.veilarbportefolje.elastic.domene;

import lombok.Data;

@Data
public class Bucket {
    String key;
    Long doc_count;
}
