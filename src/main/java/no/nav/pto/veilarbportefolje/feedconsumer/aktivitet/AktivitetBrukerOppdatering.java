package no.nav.pto.veilarbportefolje.feedconsumer.aktivitet;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatering;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;

import java.sql.Timestamp;
import java.util.Set;

@Data
@Accessors(chain = true)
public class AktivitetBrukerOppdatering implements BrukerOppdatering {
    private final String personid;
    private final String aktoerid;
    private Timestamp nyesteUtlopteAktivitet;
    private Timestamp aktivitetStart;
    private Timestamp nesteAktivitetStart;
    private Timestamp forrigeAktivitetStart;
    private Set<AktivitetStatus> aktiviteter;

    @Override
    public String getPersonid() {
        return personid;
    }

    @Override
    public Brukerdata applyTo(Brukerdata bruker) {
        return bruker
                .setAktoerid(aktoerid)
                .setNyesteUtlopteAktivitet(nyesteUtlopteAktivitet)
                .setAktivitetStart(aktivitetStart)
                .setNesteAktivitetStart(nesteAktivitetStart)
                .setForrigeAktivitetStart(forrigeAktivitetStart);
    }
}
