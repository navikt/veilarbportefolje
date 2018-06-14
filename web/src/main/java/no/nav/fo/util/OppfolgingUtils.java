package no.nav.fo.util;


public class OppfolgingUtils {
    public static boolean erBrukerUnderOppfolging(String formidlingsgruppekode, String servicegruppekode, boolean oppfolgingsbruker) {
        return oppfolgingsbruker ||
                UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, servicegruppekode);
    }

}
