package no.nav.fo.domene;


import com.google.common.collect.ImmutableSet;

import java.util.*;

public class AktivitetData {

    public static final Set<String> aktivitettyperSet = ImmutableSet.of("jobbsoeking", "egenaktivitet");

    public static final List<String> fullførteStatuser = Arrays.asList( new String[]{"fullfort", "avbrutt", "gjennomført"});
}
