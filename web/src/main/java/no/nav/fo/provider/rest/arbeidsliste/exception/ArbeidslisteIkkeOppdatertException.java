package no.nav.fo.provider.rest.arbeidsliste.exception;

import no.nav.fo.provider.rest.arbeidsliste.FeilmeldingResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ArbeidslisteIkkeOppdatertException extends WebApplicationException {

    public ArbeidslisteIkkeOppdatertException() {
        super(
                Response
                        .serverError()
                        .entity(new FeilmeldingResponse("Kunne ikke oppdatere arbeidsliste"))
                        .build()
        );
    }
}
