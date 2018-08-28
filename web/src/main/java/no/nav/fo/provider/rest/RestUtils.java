package no.nav.fo.provider.rest;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.util.function.Supplier;

import static javax.ws.rs.core.Response.Status.OK;

@Slf4j
class RestUtils {
    static Response createResponse(Supplier<Object> supplier) {
        return Try.ofSupplier(supplier)
                .toEither()
                .fold(
                        (throwable) -> {
                            log.warn("Exception ved restkall", throwable);
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
                        },
                        (entity) -> Response.status(OK).entity(entity)
                ).build();
    }
}
