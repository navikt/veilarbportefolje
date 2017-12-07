package no.nav.fo.domene;

import no.nav.fo.loependeytelser.LoependeVedtak;

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
    DAGPENGER_OVRIGE(
            (vedtak) -> "DAGP".equals(vedtak.getSakstypeKode())
                    && !"DAGO".equals(vedtak.getRettighetstypeKode())
                    && !"PERM".equals(vedtak.getRettighetstypeKode())
    ),
    AAP_MAXTID(
            (vedtak) -> "AA".equals(vedtak.getSakstypeKode())
                    && "AAP".equals(vedtak.getRettighetstypeKode())
                    && (vedtak.getAaptellere() != null
                    && vedtak.getAaptellere().getAntallDagerUnntak() == null)
                    && vedtak.getAaptellere().getAntallDagerIgjenUnntak() == null
    ),
    AAP_UNNTAK(
            (vedtak) ->
                    "AA".equals(vedtak.getSakstypeKode())
                    && "AAP".equals(vedtak.getRettighetstypeKode())
                    && vedtak.getAaptellere() != null
                    && (vedtak.getAaptellere().getAntallDagerUnntak() != null
                    || vedtak.getAaptellere().getAntallDagerIgjenUnntak() != null)
    ),
    TILTAKSPENGER(
            (vedtak) -> "INDIV".equals(vedtak.getSakstypeKode()) && "BASI".equals(vedtak.getRettighetstypeKode())
    );

    public final Predicate<LoependeVedtak> sjekk;

    YtelseMapping(Predicate<LoependeVedtak> sjekk) {
        this.sjekk = sjekk;
    }

    public static YtelseMapping of(String s) {
        if (s == null) {
            return null;
        }
        return valueOf(s);
    }

    public static Optional<YtelseMapping> of(LoependeVedtak vedtak) {
        return Stream.of(values())
                .filter((YtelseMapping mapping) -> mapping.sjekk.test(vedtak))
                .findAny();
    }
}
