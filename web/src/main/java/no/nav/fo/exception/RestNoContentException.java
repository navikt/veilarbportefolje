package no.nav.fo.exception;

import javax.ws.rs.WebApplicationException;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;

public class RestNoContentException extends WebApplicationException {
    public RestNoContentException(String s) {
        super(s, NO_CONTENT);
    }
}
