package no.nav.fo.provider.rest;

import javaslang.control.Try;
import no.nav.fo.exception.RestBadGateWayException;
import no.nav.fo.exception.RestNotFoundException;
import no.nav.fo.exception.RestTilgangException;
import no.nav.fo.exception.RestValideringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;

class RestUtils {
    private static Logger logger = LoggerFactory.getLogger(RestUtils.class);
    private static final Map<Class<? extends Throwable>, Response.Status> statusmap = new HashMap<>();

    static {
        statusmap.put(RestTilgangException.class, Response.Status.FORBIDDEN);
        statusmap.put(RestValideringException.class, Response.Status.BAD_REQUEST);
        statusmap.put(RestBadGateWayException.class, Response.Status.BAD_GATEWAY);
        statusmap.put(RestNotFoundException.class, Response.Status.NOT_FOUND);
    }

    static Response createResponse(Try.CheckedSupplier<Object> supplier) {
        return createResponse(supplier, OK);
    }

    static Response createResponse(Try<Object> supplier) {
        return createResponse(supplier, OK);
    }

    static Response createResponse(Try.CheckedSupplier<Object> supplier, Response.Status status) {
        return createResponse(Try.of(supplier), status);
    }

    static Response createResponse(Try<Object> response, Response.Status status) {
        return response
                .toEither()
                .fold(
                        (throwable) -> {
                            logger.warn("Exception ved restkall", throwable);
                            return Response.status(statusmap.getOrDefault(throwable.getClass(), Response.Status.INTERNAL_SERVER_ERROR)).entity(throwable.getMessage());
                        },
                        (entity) -> Response.status(status).entity(entity)
                ).build();
    }
}
