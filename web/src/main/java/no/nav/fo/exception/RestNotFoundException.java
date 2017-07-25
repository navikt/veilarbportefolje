package no.nav.fo.exception;

public class RestNotFoundException extends RuntimeException {
    public RestNotFoundException(String s) {
        super(s);
    }
}
