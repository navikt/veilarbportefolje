package no.nav.fo.util;

public class UnderOppfolgingRegler {

    public static boolean erUnderOppfolging(String formidlingsgruppekode, String servicegruppekode) {
        return !(formidlingsgruppekode.equals("ISERV") ||
                (formidlingsgruppekode.equals("IARBS") && (servicegruppekode.equals("BKART")
                        || servicegruppekode.equals("IVURD") || servicegruppekode.equals("KAP11")
                        || servicegruppekode.equals("VARIG") || servicegruppekode.equals("VURDI"))));
    }

}
