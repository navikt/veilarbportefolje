package no.nav.pto.veilarbportefolje.sisteendring;


public enum SisteEndringsKategorier {
    MAL,

    NY_STILLING,
    NY_IJOBB,
    NY_EGEN,
    NY_BEHANDLING,

    FULLFORT_STILLING,
    FULLFORT_IJOBB,
    FULLFORT_EGEN,
    FULLFORT_BEHANDLING,

    AVBRUTT_STILLING,
    AVBRUTT_IJOBB,
    AVBRUTT_EGEN,
    AVBRUTT_BEHANDLING;

    public static boolean contains(String value) {
        try {
            SisteEndringsKategorier.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
