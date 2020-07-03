package no.nav.pto.veilarbportefolje.util;

import no.nav.common.utils.Pair;

import java.util.*;

public class CollectionUtils {
    public static <K, V> Map<K, V> mapOf(Pair<K,V>... varargs) {

        Map<K, V> map = new HashMap<>();
        for (Pair<K, V> vararg : varargs) {
            map.put(vararg.getFirst(), vararg.getSecond());
        }
        return map;
    }
    public static <T> List<T> listOf(T... varargs) {
        return Arrays.asList(varargs);
    }

    public static <T> List<T> listOf(T singleton) {
        return Collections.singletonList(singleton);
    }

    public static <T> Set<T> setOf(T... varargs) {
        List<T> list = Arrays.asList(varargs);
        return new HashSet<>(list);
    }

}
