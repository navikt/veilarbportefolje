package no.nav.fo.util;

class FodselsnummerUtils {
    private static final String DATO_POSTFIX = "T00:00:00Z";

    static int lagFodselsdagIMnd(String fnr) {
        if(erDNummer(fnr)) {
            fnr = konverterDNummerTilFodselsnummer(fnr);
        }
        return Integer.parseInt(fnr.substring(0,2));
    }

    static String lagFodselsdato(String fnr) {
        if(erDNummer(fnr)) {
            fnr = konverterDNummerTilFodselsnummer(fnr);
        }

        String maaned = fnr.substring(2,4);

        return lagAarstallFraIndividnummer(fnr) + "-" + maaned + "-" + lagFodselsdagIMnd(fnr) + DATO_POSTFIX;
    }

    static String lagKjonn(String fnr) {
        int kjonnsNr = Integer.parseInt(fnr.substring(8,9));
        return kjonnsNr % 2 == 0 ? "K" : "M";
    }

    static boolean erNyBruker(String veilederid) {
        if(veilederid == null) {
            return true;
        }
        return false;
    }

    private static boolean erDNummer(String fnr) {
         return Integer.parseInt(fnr.substring(0,1)) > 3;
    }

    private static String konverterDNummerTilFodselsnummer(String dnr) {
        int forsteSiffer = Integer.parseInt(dnr.substring(0,1));
        forsteSiffer -= 4;
        return forsteSiffer + dnr.substring(1);
     }

    private static int lagAarstallFraIndividnummer(String fnr) {
         int individnummer = Integer.parseInt(fnr.substring(6,9));
         int aarstall = Integer.parseInt(fnr.substring(4,6));

         return individnummer > 499 && aarstall < 40 ? aarstall + 2000 : aarstall + 1900;
    }
}
