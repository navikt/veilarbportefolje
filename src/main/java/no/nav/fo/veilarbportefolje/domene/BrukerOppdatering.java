package no.nav.fo.veilarbportefolje.domene;

import java.util.Set;

public interface BrukerOppdatering {
    String getPersonid();
    Brukerdata applyTo(Brukerdata bruker);

    default Set<AktivitetStatus> getAktiviteter() {
        return null;
    }

}
