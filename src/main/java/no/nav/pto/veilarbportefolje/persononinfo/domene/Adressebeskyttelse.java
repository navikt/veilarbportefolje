package no.nav.pto.veilarbportefolje.persononinfo.domene;

import no.nav.common.client.norg2.Norg2Client;

import static no.nav.common.client.norg2.Norg2Client.Diskresjonskode.SPFO;
import static no.nav.common.client.norg2.Norg2Client.Diskresjonskode.SPSF;

public enum Adressebeskyttelse {

    UGRADERT("UGRADERT", null, null),
    FORTROLIG("FORTROLIG", "7", SPFO),
    STRENGT_FORTROLIG("STRENGT_FORTROLIG", "6", SPSF),
    STRENGT_FORTROLIG_UTLAND("STRENGT_FORTROLIG_UTLAND", "19", SPSF);

    public final String navn;
    public final String diskresjonskode;
    public final Norg2Client.Diskresjonskode norgKode;

    Adressebeskyttelse(String navn, String diskresjonskode, Norg2Client.Diskresjonskode norgKode) {
        this.navn = navn;
        this.diskresjonskode = diskresjonskode;
        this.norgKode = norgKode;
    }

    public static Adressebeskyttelse fraDiskresjonskode(String diskresjonskode) {
        return switch (diskresjonskode) {
            case "7" -> FORTROLIG;
            case "6" -> STRENGT_FORTROLIG;
            case "19" -> STRENGT_FORTROLIG_UTLAND;
            default ->
                    throw new IllegalArgumentException("Klarte ikke å instansiere adressebeskyttelse gitt diskresjonskode: " + diskresjonskode + ". Årsak: diskresjonskoden er ukjent.");
        };
    }

    public static String mapKodeTilTall(String gradering) {
        if (UGRADERT.navn.equals(gradering)) {
            return UGRADERT.diskresjonskode;
        } else if (FORTROLIG.navn.equals(gradering)) {
            return FORTROLIG.diskresjonskode;
        } else if (STRENGT_FORTROLIG.navn.equals(gradering)) {
            return STRENGT_FORTROLIG.diskresjonskode;
        } else if (STRENGT_FORTROLIG_UTLAND.navn.equals(gradering)) {
            return STRENGT_FORTROLIG_UTLAND.diskresjonskode;
        } else {
            return null;
        }
    }
}
