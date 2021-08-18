package no.nav.pto.veilarbportefolje.domene;


import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.YtelsesInnhold;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum YtelseMapping {
    ORDINARE_DAGPENGER(
            (vedtak) -> "DAGP".equals(vedtak.getSakstypeKode()) && "DAGO".equals(vedtak.getRettighetstypeKode())
    ),
    DAGPENGER_MED_PERMITTERING(
            (vedtak) -> "DAGP".equals(vedtak.getSakstypeKode()) && "PERM".equals(vedtak.getRettighetstypeKode())
    ),
    DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI(
            (vedtak) -> "DAGP".equals(vedtak.getSakstypeKode()) && "FISK".equals(vedtak.getRettighetstypeKode())
    ),
    LONNSGARANTIMIDLER_DAGPENGER(
            (vedtak) -> "DAGP".equals(vedtak.getSakstypeKode()) && "LONN".equals(vedtak.getRettighetstypeKode())
    ),
    DAGPENGER_OVRIGE(
            (vedtak) -> "DAGP".equals(vedtak.getSakstypeKode())
                    && !"DAGO".equals(vedtak.getRettighetstypeKode())
                    && !"PERM".equals(vedtak.getRettighetstypeKode())
                    && !"FISK".equals(vedtak.getRettighetstypeKode())
                    && !"LONN".equals(vedtak.getRettighetstypeKode())
    ),
    AAP_MAXTID(
            (vedtak) -> "AA".equals(vedtak.getSakstypeKode())
                    && "AAP".equals(vedtak.getRettighetstypeKode())
                    && vedtak.getAntallDagerIgjenUnntak() == null
    ),
    // I perioden fra  unntak er innvilget og til meldekort er beregnet og teller har startet nedtelling vil ikke brukere
    // dukke opp i filtrering under "AAP Unntak". Dette kan være en periode på opptil 14 dager.
    AAP_UNNTAK(
            (vedtak) ->
                    "AA".equals(vedtak.getSakstypeKode())
                    && "AAP".equals(vedtak.getRettighetstypeKode())
                    && (vedtak.getAntallDagerIgjenUnntak() != null)
    ),
    TILTAKSPENGER(
            (vedtak) -> "INDIV".equals(vedtak.getSakstypeKode()) && "BASI".equals(vedtak.getRettighetstypeKode())
    );

    public final Predicate<YtelsesInnhold> sjekk;

    YtelseMapping(Predicate<YtelsesInnhold> sjekk) {
        this.sjekk = sjekk;
    }

    public static YtelseMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }

    public static Optional<YtelseMapping> of(YtelsesInnhold vedtak) {
        return Stream.of(values())
                .filter((YtelseMapping mapping) -> mapping.sjekk.test(vedtak))
                .findAny();
    }
}
