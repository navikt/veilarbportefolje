package no.nav.pto.veilarbportefolje.domene;

import java.util.Map;

public class FiltervalgMappers {
    private static final String PREFIX = "[NOW/DAY-"; // '/DAY' runder ned til dagen for å kunne bruke cache
    private static final String POSTFIX = "+1DAY/DAY]"; // NOW+1DAY/DAY velger midnatt som kommer istedenfor midnatt som var, '/DAY' for å bruke cache

    // Pga. at man fortsatt er f.eks 19år når man er 19år og 364 dager så ser spørringene litt rare ut i forhold til ønsket filter
    public static final Map<String, String> alder = Map.of(
            "19-og-under", PREFIX + "20YEARS+1DAY TO NOW" + POSTFIX,
            "20-24", PREFIX + "25YEARS+1DAY TO NOW-20YEARS" + POSTFIX,
            "25-29", PREFIX + "30YEARS+1DAY TO NOW-25YEARS" + POSTFIX,
            "30-39", PREFIX + "40YEARS+1DAY TO NOW-30YEARS" + POSTFIX,
            "40-49", PREFIX + "50YEARS+1DAY TO NOW-40YEARS" + POSTFIX,
            "50-59", PREFIX + "60YEARS+1DAY TO NOW-50YEARS" + POSTFIX,
            "60-66", PREFIX + "67YEARS+1DAY TO NOW-60YEARS" + POSTFIX,
            "67-70", PREFIX + "71YEARS+1DAY TO NOW-67YEARS" + POSTFIX
    );
}
