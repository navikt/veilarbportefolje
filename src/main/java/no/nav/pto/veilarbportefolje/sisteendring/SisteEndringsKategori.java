package no.nav.pto.veilarbportefolje.sisteendring;


public enum SisteEndringsKategori {
    MAL,

    NY_STILLING,
    NY_IJOBB,
    NY_EGEN,
    NY_BEHANDLING,

    FULLFORT_STILLING,
    FULLFORT_IJOBB,
    FULLFORT_EGEN,
    FULLFORT_BEHANDLING,
    FULLFORT_SOKEAVTALE,

    AVBRUTT_STILLING,
    AVBRUTT_IJOBB,
    AVBRUTT_EGEN,
    AVBRUTT_BEHANDLING,
    AVBRUTT_SOKEAVTALE;

    public static boolean contains(String value) {
        try {
            SisteEndringsKategori.valueOf(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
