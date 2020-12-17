package no.nav.pto.veilarbportefolje.domene.value;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class AktoerId extends ValueObject<String> {
    public AktoerId(String value) {
        super(value);
    }

    @JsonCreator
    public static AktoerId of(String aktoerId) {
        return new AktoerId(aktoerId);
    }
}
