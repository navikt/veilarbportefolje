package no.nav.fo.provider.rest.arbeidsliste.exception;

import no.nav.fo.provider.rest.arbeidsliste.FeilmeldingResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class ArbeidslisteIkkeFunnetException extends WebApplicationException {
    public ArbeidslisteIkkeFunnetException() {
        super(
                Response
                        .status(NOT_FOUND)
                        .entity(new FeilmeldingResponse("Kunne ikke finne arbeidsliste"))
                        .build()
        );
    }
}
