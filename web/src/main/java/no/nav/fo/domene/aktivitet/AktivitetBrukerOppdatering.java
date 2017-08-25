package no.nav.fo.domene.aktivitet;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;

import java.sql.Timestamp;
import java.util.Set;

@Data
@Accessors(chain = true)
public class AktivitetBrukerOppdatering implements BrukerOppdatering {
    private final String personid;
    private final String aktoerid;
    private Boolean iAvtaltAktivitet;
    private Timestamp nyesteUtlopteAktivitet;
    private Set<AktivitetStatus> aktiviteter;

    @Override
    public String getPersonid() {
        return personid;
    }

    @Override
    public Brukerdata applyTo(Brukerdata bruker) {
        return bruker
                .setAktiviteter(aktiviteter)
                .setAktoerid(aktoerid)
                .setIAvtaltAktivitet(iAvtaltAktivitet)
                .setNyesteUtlopteAktivitet(nyesteUtlopteAktivitet);
    }
}
