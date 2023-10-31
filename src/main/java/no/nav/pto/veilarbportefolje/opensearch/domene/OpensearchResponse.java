package no.nav.pto.veilarbportefolje.opensearch.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

public record OpensearchResponse(Hits hits) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OpensearchResponse {
    }
}
