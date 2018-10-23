package no.nav.fo.veilarbportefolje.exception;

import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.PersonId;

import static java.lang.String.format;

public class FantIkkeOppfolgingsbrukerException extends RuntimeException {
    public FantIkkeOppfolgingsbrukerException(AktoerId aktoerid) {
        super(format("Fant ikke oppfolginsbruker i arena for aktoerid %s", aktoerid));
    }

    public FantIkkeOppfolgingsbrukerException(PersonId personId) {
        super(format("Fant ikke oppfolginsbruker med personid: %s", personId));
    }

}
