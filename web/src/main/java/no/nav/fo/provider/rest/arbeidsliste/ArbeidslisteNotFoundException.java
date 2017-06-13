package no.nav.fo.provider.rest.arbeidsliste;

import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

class ArbeidslisteNotFoundException extends WebApplicationException {
    ArbeidslisteNotFoundException() {
        super(NOT_FOUND);
    }
}
