package no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste.exception;

import no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste.FeilmeldingResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ArbeidslisteIkkeOpprettetException extends WebApplicationException {
    public ArbeidslisteIkkeOpprettetException() {
        super(
                Response
                        .serverError()
                        .entity(new FeilmeldingResponse("Kunne ikke opprette arbeidsliste"))
                        .build()
        );
    }
}
