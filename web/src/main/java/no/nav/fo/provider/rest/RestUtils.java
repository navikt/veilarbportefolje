package no.nav.fo.provider.rest;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.fo.exception.*;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static javax.ws.rs.core.Response.Status.OK;

@Slf4j
class RestUtils {
    private static final Map<Class<? extends Throwable>, Response.Status> statusmap = new HashMap<>();

    static {
        statusmap.put(IngenTilgang.class, Response.Status.FORBIDDEN);
        statusmap.put(RestValideringException.class, Response.Status.BAD_REQUEST);
        statusmap.put(RestBadGateWayException.class, Response.Status.BAD_GATEWAY);
        statusmap.put(RestNotFoundException.class, Response.Status.NOT_FOUND);
        statusmap.put(RestNoContentException.class, Response.Status.NO_CONTENT);
    }

    static Response createResponse(Supplier<Object> supplier) {
        return createResponse(Try.ofSupplier(supplier), OK);
    }

    static Response createResponse(Supplier<Object> supplier, Response.Status status) {
        return createResponse(Try.ofSupplier(supplier), status);
    }

    static Response createResponse(Try<Object> response, Response.Status status) {
        return response
                .toEither()
                .fold(
                        (throwable) -> {
                            log.warn("Exception ved restkall", throwable);
                            return Response.status(statusmap.getOrDefault(throwable.getClass(), Response.Status.INTERNAL_SERVER_ERROR)).entity(throwable.getMessage());
                        },
                        (entity) -> Response.status(status).entity(entity)
                ).build();
    }
}
