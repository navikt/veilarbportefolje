package no.nav.pto.veilarbportefolje.arenapakafka;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ArenaDato {
    private static final int localDateLength = "0000-00-00".length();
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String dato;

    @JsonCreator
    public ArenaDato(String dato) {
        this.dato = dato;
    }

    /*
        NB: Klokkelsett i arena datoer kan ikke stoles pa.
     */
    public ZonedDateTime getDato() {
        if (dato.length() == localDateLength) {
            return ZonedDateTime.of(LocalDate.parse(dato).atStartOfDay(), ZoneId.systemDefault());
        }
        return ZonedDateTime.of(LocalDateTime.parse(dato, format).toLocalDate().atStartOfDay(), ZoneId.systemDefault());
    }

    public LocalDateTime getLocalDateTime() {
        if (dato.length() == localDateLength) {
            return LocalDate.parse(dato).atStartOfDay();
        }
        return LocalDateTime.parse(dato, format).toLocalDate().atStartOfDay();
    }
    public LocalDate getLocalDate() {
        if (dato.length() == localDateLength) {
            return LocalDate.parse(dato);
        }
        return LocalDate.parse(dato, format);
    }

    public static ArenaDato of(ZonedDateTime dato) {
        // TODO
        return null;
    }
}
