package no.nav.pto.veilarbportefolje.domene;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;

public class FiltervalgMappers {
    private static String prefix = "[NOW/DAY-"; // '/DAY' runder ned til dagen for å kunne bruke cache
    private static String postfix = "+1DAY/DAY]"; // NOW+1DAY/DAY velger midnatt som kommer istedenfor midnatt som var, '/DAY' for å bruke cache

    // Pga. at man fortsatt er f.eks 19år når man er 19år og 364 dager så ser spørringene litt rare ut i forhold til ønsket filter
    public static final Map<String, String> alder = new HashMap<String, String>() {{
        put("19-og-under", prefix + "20YEARS+1DAY TO NOW" + postfix);
        put("20-24", prefix + "25YEARS+1DAY TO NOW-20YEARS" + postfix);
        put("25-29", prefix + "30YEARS+1DAY TO NOW-25YEARS" + postfix);
        put("30-39", prefix + "40YEARS+1DAY TO NOW-30YEARS" + postfix);
        put("40-49", prefix + "50YEARS+1DAY TO NOW-40YEARS" + postfix);
        put("50-59", prefix + "60YEARS+1DAY TO NOW-50YEARS" + postfix);
        put("60-66", prefix + "67YEARS+1DAY TO NOW-60YEARS" + postfix);
        put("67-70", prefix + "71YEARS+1DAY TO NOW-67YEARS" + postfix);
    }};

    public static final boolean isValidDynamicRange(String fraTilAlderIput){
        String[] fraTilAlder = fraTilAlderIput.split("-");
        if (fraTilAlder.length == 2 && parseInt(fraTilAlder[0]) <= parseInt(fraTilAlder[1])){
            return true;
        }
        return false;
    }
}
