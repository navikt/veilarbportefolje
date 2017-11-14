package no.nav.fo.domene.aktivitet;

import java.util.List;

import static java.util.Arrays.asList;

public class AktivitetData {

    public static final List<AktivitetTyper> aktivitetTyperList = asList(AktivitetTyper.values());

    public static final List<AktivitetTyperFraAktivitetspan> aktivitetTyperFraAktivitetsplanList = asList(AktivitetTyperFraAktivitetspan.values());

    public static final List<AktivitetFullfortStatuser> fullførteStatuser = asList(AktivitetFullfortStatuser.values());
}
