package no.nav.fo.util;




public class OppfolgingUtils {

    public static boolean skalArbeidslisteSlettes(String gammelVeileder, String nyVeileder, boolean oppfolginsbruker) {
               return nyVeileder == null || !oppfolginsbruker || !nyVeileder.equals(gammelVeileder);
    }

    public static boolean erBrukerUnderOppfolging(String formidlingsgruppekode, String servicegruppekode, boolean oppfolgingsbruker ) {
        return oppfolgingsbruker ||
                UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, servicegruppekode);
    }


}
