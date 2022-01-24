package no.nav.pto.veilarbportefolje.opensearch.domene;

import lombok.Data;

@Data
public class Hit {
    String _index;
    String _id;
    OppfolgingsBruker _source;
}

