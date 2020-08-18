package no.nav.pto.veilarbportefolje.util;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

@Slf4j
public class RestUtils {
    public static ResponseEntity createResponse(Supplier<Object> supplier) {
        return (ResponseEntity) Try.ofSupplier(supplier)
                .toEither()
                .fold(
                        (throwable) -> {
                            //TODO VAD ER MOTSVARIGHETEN I SPRING
                            /*if (throwable instanceof WebApplicationException) {
                                return ((WebApplicationException) throwable).getResponse();
                            }

                             */
                            log.warn("Exception ved restkall", throwable);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
                        },
                        (entity) -> ResponseEntity.status(HttpStatus.OK).body(entity)
                );
    }
}
