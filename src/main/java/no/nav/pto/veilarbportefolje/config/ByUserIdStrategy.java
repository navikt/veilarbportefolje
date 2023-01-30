package no.nav.pto.veilarbportefolje.config;

import lombok.RequiredArgsConstructor;
import no.finn.unleash.strategy.Strategy;
import no.nav.common.auth.context.AuthContextHolder;

import java.util.Arrays;
import java.util.Map;
@RequiredArgsConstructor
public class ByUserIdStrategy implements Strategy {

	private final AuthContextHolder authContextHolder;

	@Override
	public String getName() {
		return "byUserId";
	}

	@Override
	public boolean isEnabled(Map<String, String> toggleUsers) {
		if (toggleUsers.get("user").isEmpty()) {
			return false;
		}

		if (authContextHolder.getNavIdent().isPresent()) {
			String navIdentIKontekst = authContextHolder.getNavIdent().get().get();
			return Arrays.asList(toggleUsers.get("user").split(",")).contains(navIdentIKontekst);
		}

		return false;
	}
}
