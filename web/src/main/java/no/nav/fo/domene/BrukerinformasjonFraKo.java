package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Accessors(chain = true)
public class BrukerinformasjonFraKo implements BrukerOppdatering {
    private String veileder;
    private String oppdatert;
    private String personid;
    private String aktoerid;

    public Brukerdata applyTo(Brukerdata brukerdata) {
        return brukerdata
                .setAktoerid(aktoerid)
                .setPersonid(personid)
                .setVeileder(veileder)
                .setTildeltTidspunkt(convertToLocalDateTime(oppdatert));
    }

    public static LocalDateTime convertToLocalDateTime(String timestamp) {
        String pattern = "yyyy-MM-dd HH:mm:ss.n";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(timestamp, formatter);

    }
}

