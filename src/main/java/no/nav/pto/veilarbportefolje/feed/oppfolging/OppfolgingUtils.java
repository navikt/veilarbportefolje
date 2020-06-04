package no.nav.pto.veilarbportefolje.feed.oppfolging;

import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

public class OppfolgingUtils {
    private static List<String> INNSATSGRUPPEKODER =  asList( "IKVAL", "BFORM", "BATT", "VARIG");
    private static List<String> OPPFOLGINGKODER = asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG");

    public static boolean isNyForEnhet(String veileder) {
        return veileder == null || veileder.isEmpty();
    }


    //TODO BRUK PROFILERINGSRESULTAT
    public static boolean trengerVurdering(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return false;
        }
        return "IVURD".equals(kvalifiseringsgruppekode) || "BKART".equals(kvalifiseringsgruppekode);
    }

    public static boolean erSykmeldtMedArbeidsgiver (String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        return "IARBS".equals(formidlingsgruppekode) && !OPPFOLGINGKODER.contains(kvalifiseringsgruppekode);
    }

    public static boolean trengerRevurderingVedtakstotte (String formidlingsgruppekode, String kvalifiseringsgruppekode, String vedtakStatus) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return false;
        }
        return INNSATSGRUPPEKODER.contains(kvalifiseringsgruppekode) && vedtakStatus != null;
    }

    public static VurderingsBehov vurderingsBehov(String formidlingsgruppekode, String kvalifiseringsgruppekode, String profileringsResultat, boolean erVedtakstottePilotPa) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return null;
        }

        //kvalifiseringsgruppekodeTilVurdering brukes fordi inte alla brukare har aktorId og dærmed inte har profileringsresultat
        return Optional.ofNullable(profileringsResultatTilVurdering(profileringsResultat, erVedtakstottePilotPa))
                .orElse(kvalifiseringsgruppekodeTilVurdering(kvalifiseringsgruppekode));
    }


    private static VurderingsBehov profileringsResultatTilVurdering (String profileringsResultat, boolean erVedtakstottePilotPa) {
        return Optional.ofNullable(profileringsResultat)
                .map(ProfilertTil::valueOf)
                .map(profilertTil -> {
                    if(erVedtakstottePilotPa) {
                        return VurderingsBehov.valueOf(profilertTil.name());
                    } else {
                        return profilertTil.equals(ProfilertTil.OPPGITT_HINDRINGER) ? VurderingsBehov.ARBEIDSEVNE_VURDERING : VurderingsBehov.IKKE_VURDERT;
                    }
                })
                .orElse(null);
    }

    private static VurderingsBehov kvalifiseringsgruppekodeTilVurdering (String kvalifiseringsgruppekode) {
        if ("IVURD".equals(kvalifiseringsgruppekode)) {
            return VurderingsBehov.IKKE_VURDERT;
        } else if ("BKART".equals(kvalifiseringsgruppekode)) {
            return VurderingsBehov.ARBEIDSEVNE_VURDERING;
        } else {
            return null;
        }
    }
}
