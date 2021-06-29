package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class ArenaDato {
    private static final int localDateLength = "0000-00-00".length();
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String dato;

    @JsonCreator
    public ArenaDato(String dato) {
        this.dato = dato;
    }

    public ZonedDateTime getDato() {
        return getDato(false);
    }

    public ZonedDateTime getDato(boolean tilOgMedDato) {
        if (dato.length() == localDateLength) {
            if(tilOgMedDato){
                return ZonedDateTime.of(LocalDate.parse(dato).atStartOfDay(), ZoneId.of("Europe/Oslo"))
                        .plusHours(23).plusMinutes(59);
            }
            return ZonedDateTime.of(LocalDate.parse(dato).atStartOfDay(), ZoneId.of("Europe/Oslo"));
        }
        return ZonedDateTime.of(LocalDateTime.parse(dato, format), ZoneId.of("Europe/Oslo"));
    }
}
