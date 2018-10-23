package no.nav.fo.domene;

import lombok.Getter;
import lombok.Value;
import no.nav.fo.exception.UgyldigFnrException;

import static lombok.AccessLevel.PRIVATE;

@Value
@Getter(value = PRIVATE)
public class Fnr {
    String fnr;

    public Fnr(String fnr) {
        if (fnr.matches("\\d{11}")) {
            this.fnr = fnr;
        } else {
            throw new UgyldigFnrException(fnr);
        }
    }

    public static Fnr of(String fnr) {
        return new Fnr(fnr);
    }

    @Override
    public String toString() {
        return fnr;
    }
}
