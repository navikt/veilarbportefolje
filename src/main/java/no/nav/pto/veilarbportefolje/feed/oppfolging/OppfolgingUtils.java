package no.nav.pto.veilarbportefolje.feed.oppfolging;

import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;

import java.util.List;

import static java.util.Arrays.asList;

public class OppfolgingUtils {
    private static List<String> INNSATSGRUPPEKODER =  asList( "IKVAL", "BFORM", "BATT", "VARIG");
    private static List<String> OPPFOLGINGKODER = asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG");
    public static boolean erBrukerUnderOppfolging(String formidlingsgruppekode, String servicegruppekode, boolean oppfolgingsbruker) {
        return oppfolgingsbruker ||
                UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, servicegruppekode);
    }

    public static boolean isNyForEnhet(String veileder) {
        return veileder == null || veileder.isEmpty();
    }

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

    public static VurderingsBehov vurderingsBehov(String formidlingsgruppekode, String kvalifiseringsgruppekode) {
        if ("ISERV".equals(formidlingsgruppekode)) {
            return null;
        } else if ("IVURD".equals(kvalifiseringsgruppekode)) {
            return VurderingsBehov.IKKE_VURDERT;
        } else if ("BKART".equals(kvalifiseringsgruppekode)) {
            return VurderingsBehov.ARBEIDSEVNE_VURDERING;
        } else {
            return null;
        }
    }
}
