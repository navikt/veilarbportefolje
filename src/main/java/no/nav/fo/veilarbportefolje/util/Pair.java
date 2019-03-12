package no.nav.fo.veilarbportefolje.util;

import lombok.Value;

@Value(staticConstructor = "of")
public class Pair<K, V> {
    K first;
    V second;
}
