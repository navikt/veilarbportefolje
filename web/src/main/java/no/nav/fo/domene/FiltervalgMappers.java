package no.nav.fo.domene;

public class FiltervalgMappers {
    private static String prefix = "[NOW/DAY-"; // '/DAY' runder ned til dagen for å kunne bruke cache
    private static String postfix = "+1DAY/DAY]"; // NOW+1DAY/DAY velger midnatt som kommer istedenfor midnatt som var, '/DAY' for å bruke cache

    // Pga. at man fortsatt er f.eks 19år når man er 19år og 364 dager så ser spørringene litt rare ut i forhold til ønsket filter
    public static final String[] alder = new String[] {
        prefix + "20YEARS+1DAY TO NOW" + postfix,
        prefix + "25YEARS+1DAY TO NOW-20YEARS" + postfix,
        prefix + "30YEARS+1DAY TO NOW-25YEARS" + postfix,
        prefix + "40YEARS+1DAY TO NOW-30YEARS" + postfix,
        prefix + "50YEARS+1DAY TO NOW-40YEARS" + postfix,
        prefix + "60YEARS+1DAY TO NOW-50YEARS" + postfix,
        prefix + "67YEARS+1DAY TO NOW-60YEARS" + postfix,
        prefix + "71YEARS+1DAY TO NOW-67YEARS" + postfix
    };

    public static final String[] kjonn = new String[] {
      "K", "M"
    };

    public static final String[] fodselsdagIMnd = new String[] {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
        "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"
    };

    public static final String[] innsatsgruppe = new String[] {
            "BATT", "BFORM", "IKVAL", "VARIG"
    };

    public static final String[] formidlingsgruppe = new String[] {
            "ARBS", "IARBS", "ISERV", "PARBS", "RARBS"
    };

    public static final String[] servicegruppe = new String[] {
            "BKART", "IVURD", "OPPFI", "VARIG", "VURDI", "VURDU"
    };
}
