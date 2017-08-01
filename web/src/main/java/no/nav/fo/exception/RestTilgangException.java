package no.nav.fo.exception;

import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

public class RestTilgangException extends WebApplicationException {

    public RestTilgangException(String s) {
        super(s, FORBIDDEN);
    }

}
