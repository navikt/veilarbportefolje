package no.nav.fo.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class BrukerinformasjonFraFeed implements BrukerOppdatering {
    private String veileder;
    private Timestamp oppdatert;
    private String personid;
    private String aktoerid;
    private Boolean oppfolging;
    private Boolean nyForVeileder;

    public Brukerdata applyTo(Brukerdata brukerdata) {
        return brukerdata
                .setOppfolging(oppfolging)
                .setNyForVeileder(nyForVeileder)
                .setAktoerid(aktoerid)
                .setPersonid(personid)
                .setVeileder(veileder)
                .setTildeltTidspunkt(oppdatert);
    }
}

