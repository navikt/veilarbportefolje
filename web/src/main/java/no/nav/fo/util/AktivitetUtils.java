package no.nav.fo.util;

import java.util.List;
import java.util.stream.Collectors;

public class AktivitetUtils {
    public static Boolean erBrukersAktivitetAktiv(List<String> aktivitetStatusListe, List<String> fullførsteStatuser) {
        return !aktivitetStatusListe
                .stream()
                .filter( status -> !fullførsteStatuser.contains(status))
                .collect(Collectors.toList())
                .isEmpty();
    }
}
