package no.nav.fo.config.unleash;

import java.util.Optional;

public class EnvironmentUtils {

    private static final String FASIT_ENVIRONMENT_NAME_PROPERTY_NAME = "FASIT_ENVIRONMENT_NAME";
    private static final String FASIT_ENVIRONMENT_NAME_PROPERTY_NAME_SKYA = "environment.name";

    public static Optional<String> getEnvironmentName() {
        return no.nav.sbl.util.EnvironmentUtils.getOptionalProperty(FASIT_ENVIRONMENT_NAME_PROPERTY_NAME, FASIT_ENVIRONMENT_NAME_PROPERTY_NAME_SKYA);
    }

}
