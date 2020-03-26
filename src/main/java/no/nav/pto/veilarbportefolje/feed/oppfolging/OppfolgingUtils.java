package no.nav.pto.veilarbportefolje.feed.oppfolging;


import no.nav.pto.veilarbportefolje.domene.VurderingsBehov;
import no.nav.pto.veilarbportefolje.registrering.DinSituasjonSvar;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

public class OppfolgingUtils {
    private static List<String> INNSATSGRUPPEKODER =  asList( "IKVAL", "BFORM", "BATT", "VARIG");
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
        List<String> OPPFOLGINGKODER = asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG");
        return "IARBS".equals(formidlingsgruppekode) && !OPPFOLGINGKODER.contains(kvalifiseringsgruppekode);
    }

    public static boolean trengerVurderingVedtakstotte (String kvalifiseringsgruppekode, String vedtakStatus) {
        return !INNSATSGRUPPEKODER.contains(kvalifiseringsgruppekode) && vedtakStatus == null;
    }

    public static boolean trengerRevurderingVedtakstotte (String kvalifiseringsgruppekode, String vedtakStatus) {

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

    public static boolean erPermitertUtenOppfolgingsvedtak(String formidlingsgruppekode, String brukersSituasjon, String kvalifiseringsgruppekode) {
        return Optional.ofNullable(brukersSituasjon)
                .map(DinSituasjonSvar::valueOf)
                .map(dinSituasjonSvar ->
                        "ARBS".equals(formidlingsgruppekode)
                                && !INNSATSGRUPPEKODER.contains(kvalifiseringsgruppekode)
                                && dinSituasjonSvar.equals(DinSituasjonSvar.ER_PERMITTERT))
                .orElse(false);
    }
}
