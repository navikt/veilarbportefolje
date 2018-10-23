package no.nav.fo.config.unleash;

import no.finn.unleash.strategy.Strategy;

import java.util.Arrays;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static no.nav.fo.config.unleash.EnvironmentUtils.getEnvironmentName;

public class ByEnvironmentStrategy implements Strategy {

    @Override
    public String getName() {
        return "byEnvironment";
    }

    @Override
    public boolean isEnabled(Map<String, String> map) {
        return ofNullable(map)
                .map(m -> m.get("miljø")).map(s -> s.split(","))
                .map(Arrays::stream)
                .map(s -> s.anyMatch(this::isEnvironment))
                .orElse(false);
    }

    private boolean isEnvironment(String environment) {
        return getEnvironmentName().map(environment::equals).orElse(false);
    }

}