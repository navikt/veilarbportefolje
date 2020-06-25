package no.nav.pto.veilarbportefolje.domene;

import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetStatus;

import java.util.Set;

public interface BrukerOppdatering {
    String getPersonid();
    Brukerdata applyTo(Brukerdata bruker);

    default Set<AktivitetStatus> getAktiviteter() {
        return null;
    }

}
