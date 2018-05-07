package no.nav.fo.util;


import no.nav.fo.domene.Oppfolgingstatus;

import java.util.Objects;

public class OppfolgingUtils {

    public static boolean skalArbeidslisteSlettes(boolean oppfolginsbruker) {
               return !oppfolginsbruker;
    }

    public static boolean erBrukerUnderOppfolging(String formidlingsgruppekode, String servicegruppekode, boolean oppfolgingsbruker) {
        return oppfolgingsbruker ||
                UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, servicegruppekode);
    }

    public static boolean erBrukerUnderOppfolging(Oppfolgingstatus status) {
        if(Objects.isNull(status)) {
            return false;
        }
        return OppfolgingUtils.erBrukerUnderOppfolging(
                status.getFormidlingsgruppekode(),
                status.getServicegruppekode(),
                status.isOppfolgingsbruker()
        );
    }
}
