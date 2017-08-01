package no.nav.fo.exception;

import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.PersonId;

import static java.lang.String.format;

public class FantIkkeOppfolgingsbrukerException extends RuntimeException {
    public FantIkkeOppfolgingsbrukerException(AktoerId aktoerid) {
        super(format("Fant ikke oppfolginsbruker i arena for aktoerid", aktoerid.toString()));
    }

    public FantIkkeOppfolgingsbrukerException(PersonId personId) {
        super(format("Fant ikke oppfolginsbruker med personid", personId.toString()));
    }


}
