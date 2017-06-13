package no.nav.fo.service;

import no.nav.fo.domene.Arbeidsliste;

import java.sql.Timestamp;
import java.util.Optional;

public interface ArbeidslisteService {
    public Optional<Arbeidsliste> getArbeidsliste(String fnr);

    public Optional<Arbeidsliste> createArbeidsliste(String fnr, String kommentar, Timestamp dato);

    public Optional<Arbeidsliste> updateArbeidsliste(String fnr, String kommentar, Timestamp dato);

    public Optional<Arbeidsliste> deleteArbeidsliste(String fnr);
}
