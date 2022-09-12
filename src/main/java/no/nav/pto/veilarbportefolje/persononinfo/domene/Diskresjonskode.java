package no.nav.pto.veilarbportefolje.persononinfo.domene;

import no.nav.common.client.norg2.Norg2Client;

import static no.nav.common.client.norg2.Norg2Client.Diskresjonskode.SPFO;
import static no.nav.common.client.norg2.Norg2Client.Diskresjonskode.SPSF;

public enum Diskresjonskode {

    UGRADERT("UGRADERT", null, null),
    FORTROLIG("FORTROLIG", "7", SPFO),
    STRENGT_FORTROLIG("STRENGT_FORTROLIG", "6", SPSF),
    STRENGT_FORTROLIG_UTLAND("STRENGT_FORTROLIG_UTLAND", "19", SPSF);

    public final String kode;
    public final String tallVerdi;
    public final Norg2Client.Diskresjonskode norgKode;

    Diskresjonskode(String kode, String tallVerdi, Norg2Client.Diskresjonskode norgKode) {
        this.kode = kode;
        this.tallVerdi = tallVerdi;
        this.norgKode = norgKode;
    }

    public static Diskresjonskode fraTall(String tallVerdi) {
        switch (tallVerdi) {
            case "7":
                return FORTROLIG;
            case "6":
                return STRENGT_FORTROLIG;
            case "19":
                return STRENGT_FORTROLIG_UTLAND;
            default:
                throw new IllegalArgumentException("Fant ikke " + Diskresjonskode.class.getCanonicalName() + " med tallverdi: " + tallVerdi);
        }
    }

    public static String mapKodeTilTall(String gradering) {
        if (UGRADERT.kode.equals(gradering)) {
            return UGRADERT.tallVerdi;
        } else if (FORTROLIG.kode.equals(gradering)) {
            return FORTROLIG.tallVerdi;
        } else if (STRENGT_FORTROLIG.kode.equals(gradering)) {
            return STRENGT_FORTROLIG.tallVerdi;
        } else if (STRENGT_FORTROLIG_UTLAND.kode.equals(gradering)) {
            return STRENGT_FORTROLIG_UTLAND.tallVerdi;
        } else {
            return null;
        }
    }
}
