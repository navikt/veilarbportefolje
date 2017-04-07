package no.nav.fo.domene;

public interface BrukerOppdatering {
    String getPersonid();
    Brukerdata applyTo(Brukerdata bruker);
}
