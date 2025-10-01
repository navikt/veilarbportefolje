package no.nav.pto.veilarbportefolje.config;

import io.getunleash.UnleashContext;
import lombok.RequiredArgsConstructor;
import io.getunleash.strategy.Strategy;
import no.nav.common.auth.context.AuthContextHolder;

import java.util.Map;

@RequiredArgsConstructor
public class ByUserIdStrategy implements Strategy {

	private final AuthContextHolder authContextHolder;

	@Override
	public String getName() {
		return "byUserId";
	}

	/**
	 * @param map
	 * @param unleashContext
	 * @return
	 */
	@Override
	public boolean isEnabled(Map<String, String> map, UnleashContext unleashContext) {
		return false;
	}

}
