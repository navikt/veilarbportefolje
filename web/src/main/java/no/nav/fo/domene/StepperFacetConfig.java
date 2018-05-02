package no.nav.fo.domene;

import lombok.Value;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

@Value
public class StepperFacetConfig {
    public static final List<YtelseFilter> DAGPENGER_VALG = asList(
            YtelseFilter.ORDINARE_DAGPENGER,
            YtelseFilter.DAGPENGER_MED_PERMITTERING,
            YtelseFilter.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI,
            YtelseFilter.LONNSGARANTIMIDLER_DAGPENGER,
            YtelseFilter.DAGPENGER_OVRIGE,
            YtelseFilter.DAGPENGER
    );

    int min;
    int step;
    int max;
    YtelseFilter ytelse;

    public static Optional<StepperFacetConfig> stepperFacetConfig(YtelseFilter ytelse) {
        if (ytelse == YtelseFilter.AAP_MAXTID) {
            return Optional.of(new StepperFacetConfig(2, 8, 215, ytelse));
        } else if (ytelse == YtelseFilter.AAP_UNNTAK) {
            return Optional.of(new StepperFacetConfig(2, 4, 104, ytelse));
        } else if (DAGPENGER_VALG.contains(ytelse)) {
            return Optional.of(new StepperFacetConfig(2, 3, 52, ytelse));
        } else {
            return Optional.empty();
        }
    }
}
