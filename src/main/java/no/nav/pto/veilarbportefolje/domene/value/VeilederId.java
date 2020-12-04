package no.nav.pto.veilarbportefolje.domene.value;

import com.fasterxml.jackson.annotation.JsonCreator;

public final class VeilederId extends ValueObject<String>{
    public VeilederId(String value) {
        super(value);
    }

    @JsonCreator
    public static VeilederId of(String value) {
        return new VeilederId(value);
    }
}
