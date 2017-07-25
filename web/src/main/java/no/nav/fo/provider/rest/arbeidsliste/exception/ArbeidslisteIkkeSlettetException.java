package no.nav.fo.provider.rest.arbeidsliste.exception;

import no.nav.fo.provider.rest.arbeidsliste.FeilmeldingResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ArbeidslisteIkkeSlettetException extends WebApplicationException {

    public ArbeidslisteIkkeSlettetException() {
        super(
                Response
                        .serverError()
                        .entity(new FeilmeldingResponse("Kunne ikke slette arbeidsliste"))
                        .build()
        );
    }
}
