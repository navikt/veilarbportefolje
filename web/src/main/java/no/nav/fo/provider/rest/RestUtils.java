package no.nav.fo.provider.rest;

import javaslang.control.Try;
import no.nav.fo.exception.RestTilgangException;
import no.nav.fo.exception.RestValideringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

class RestUtils {
    private static Logger logger = LoggerFactory.getLogger(RestUtils.class);
    private static final Map<Class<? extends Throwable>, Response.Status> statusmap = new HashMap<>();

    static {
        statusmap.put(RestTilgangException.class, Response.Status.FORBIDDEN);
        statusmap.put(RestValideringException.class, Response.Status.BAD_REQUEST);
    }

    static Response createResponse(Try.CheckedSupplier<Object> supplier) {
        return createResponse(Try.of(supplier));
    }

    static Response createResponse(Try<Object> response) {
        return response
                .toEither()
                .fold(
                        (throwable) -> {
                            logger.warn("Exception ved restkall", throwable);
                            return Response.status(statusmap.getOrDefault(throwable.getClass(), Response.Status.INTERNAL_SERVER_ERROR));
                        },
                        (entity) -> Response.status(200).entity(entity)
                ).build();
    }
}
