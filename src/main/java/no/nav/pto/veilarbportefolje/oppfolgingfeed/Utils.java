package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import java.util.List;

import static java.util.Arrays.asList;

public class Utils {

    public static List<String> getCommaSeparatedUsers(String users) {
        return asList(users.trim().toLowerCase().split(","));
    }
}
