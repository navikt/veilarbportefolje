package no.nav.pto.veilarbportefolje.util;

import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil;
import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

public class OppfolgingUtils {
    public static final List<String> INNSATSGRUPPEKODER = asList("IKVAL", "BFORM", "BATT", "VARIG");
    private static List<String> OPPFOLGINGKODER = asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG");


    //TODO BRUK PROFILERINGSRESULTAT
    public static boolean trengerVurdering(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return false;
        }
        return "IVURD".equals(kvalifiseringsgruppekode) || "BKART".equals(kvalifiseringsgruppekode);
    }

    public static boolean erSykmeldtMedArbeidsgiver(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        return "IARBS".equals(formidlingsgruppekode) && kvalifiseringsgruppekode.equals("VURDI");
    }

    public static VurderingsBehov vurderingsBehov(String formidlingsgruppekode, String kvalifiseringsgruppekode, String profileringsResultat) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return null;
        }

        //kvalifiseringsgruppekodeTilVurdering brukes fordi inte alla brukare har aktorId og dærmed inte har profileringsresultat
        return Optional.ofNullable(profileringsResultatTilVurdering(profileringsResultat))
                .orElse(kvalifiseringsgruppekodeTilVurdering(kvalifiseringsgruppekode));
    }


    private static VurderingsBehov profileringsResultatTilVurdering(String profileringsResultat) {
        return Optional.ofNullable(profileringsResultat)
                .map(ProfilertTil::valueOf)
                .map(profilertTil -> VurderingsBehov.valueOf(profilertTil.name()))
                .orElse(null);
    }

    private static VurderingsBehov kvalifiseringsgruppekodeTilVurdering(String kvalifiseringsgruppekode) {
        if ("IVURD".equals(kvalifiseringsgruppekode)) {
            return VurderingsBehov.IKKE_VURDERT;
        } else if ("BKART".equals(kvalifiseringsgruppekode)) {
            return VurderingsBehov.ARBEIDSEVNE_VURDERING;
        } else {
            return null;
        }
    }
}
