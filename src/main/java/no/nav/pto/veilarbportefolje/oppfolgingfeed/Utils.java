package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

public class Utils {

    public static <S, T> Supplier<T> apply(Function<S, T> fn, S arg) {
        return () -> fn.apply(arg);
    }

    public static List<String> getCommaSeparatedUsers(String users) {
        return asList(users.trim().toLowerCase().split(","));
    }
}
