package no.nav.fo.veilarbportefolje.config.feed;

import java.util.function.Function;
import java.util.function.Supplier;

public class Utils {

    public static <S, T> Supplier<T> apply(Function<S, T> fn, S arg) {
        return () -> fn.apply(arg);
    }

}
