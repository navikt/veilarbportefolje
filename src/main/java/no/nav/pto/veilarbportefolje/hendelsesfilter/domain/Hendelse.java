package no.nav.pto.veilarbportefolje.hendelsesfilter.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Hendelse(
        String personID,
        String avsender,
        Kategori kategori,
        Operasjon operasjon,
        @JsonProperty(value = "hendelse")
        HendelseInnhold hendelseInnhold
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Hendelse {
    }
}
