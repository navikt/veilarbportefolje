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
        if (fnr.length() != 11) {
            throw new UgyldigFnrException(fnr);
        }
        this.fnr = fnr;
    }

    @Override
    public String toString() {
        return fnr;
    }
}
