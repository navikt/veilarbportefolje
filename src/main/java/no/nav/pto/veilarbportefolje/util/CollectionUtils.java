package no.nav.pto.veilarbportefolje.util;

import no.nav.common.utils.Pair;

import java.util.HashMap;
import java.util.Map;

public class CollectionUtils {
    public static <K, V> Map<K, V> mapOf(Pair<K,V>... varargs) {

        Map<K, V> map = new HashMap<>();
        for (Pair<K, V> vararg : varargs) {
            map.put(vararg.getFirst(), vararg.getSecond());
        }
        return map;
    }
}
