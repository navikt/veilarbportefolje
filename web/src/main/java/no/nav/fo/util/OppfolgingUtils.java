package no.nav.fo.util;


import no.nav.fo.domene.*;


public class OppfolgingUtils {

    public static boolean erVeielderNyEllerOppfolgendeVeileder(Oppfolgingstatus oppfolgingstatus, String veilederId) {
        return oppfolgingstatus.getVeileder() == null || oppfolgingstatus.getVeileder().equals(veilederId);
    }

    public static boolean skalArbeidslisteSlettes(Oppfolgingstatus oppfolgingstatus, String veilederId) {
               return veilederId == null ||
                       !oppfolgingstatus.isOppfolgingsbruker() ||
                       !erVeielderNyEllerOppfolgendeVeileder(oppfolgingstatus, veilederId);
    }

    public static boolean erBrukerUnderOppfolging(Oppfolgingstatus oppfolgingstatus) {
        return oppfolgingstatus.isOppfolgingsbruker() ||
                UnderOppfolgingRegler.erUnderOppfolging(oppfolgingstatus.getFormidlingsgruppekode(), oppfolgingstatus.getServicegruppekode());
    }


}
