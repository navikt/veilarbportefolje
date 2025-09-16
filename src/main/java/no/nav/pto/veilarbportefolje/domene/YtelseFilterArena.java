package no.nav.pto.veilarbportefolje.domene;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public enum YtelseFilterArena {
    DAGPENGER(asList(YtelseMapping.ORDINARE_DAGPENGER,
            YtelseMapping.DAGPENGER_MED_PERMITTERING,
            YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI,
            YtelseMapping.LONNSGARANTIMIDLER_DAGPENGER,
            YtelseMapping.DAGPENGER_OVRIGE)),
    ORDINARE_DAGPENGER(singletonList(YtelseMapping.ORDINARE_DAGPENGER)),
    DAGPENGER_MED_PERMITTERING(singletonList(YtelseMapping.DAGPENGER_MED_PERMITTERING)),
    DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI(singletonList(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI)),
    LONNSGARANTIMIDLER_DAGPENGER(singletonList(YtelseMapping.LONNSGARANTIMIDLER_DAGPENGER)),
    DAGPENGER_OVRIGE(singletonList(YtelseMapping.DAGPENGER_OVRIGE)),
    AAP(asList(YtelseMapping.AAP_MAXTID, YtelseMapping.AAP_UNNTAK)),
    AAP_MAXTID(singletonList(YtelseMapping.AAP_MAXTID)),
    AAP_UNNTAK(singletonList(YtelseMapping.AAP_UNNTAK)),
    TILTAKSPENGER(singletonList(YtelseMapping.TILTAKSPENGER));

    public final List<YtelseMapping> underytelser;

    YtelseFilterArena(List<YtelseMapping> underytelser) {
        this.underytelser = underytelser;
    }

}
