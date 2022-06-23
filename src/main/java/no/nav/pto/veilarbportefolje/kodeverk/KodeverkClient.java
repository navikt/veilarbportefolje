package no.nav.pto.veilarbportefolje.kodeverk;

import no.nav.common.health.HealthCheck;

import java.util.Map;

public interface KodeverkClient extends HealthCheck {

    Map<String, String> hentKodeverkBeskrivelser(String kodeverksnavn);

}
