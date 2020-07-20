package no.nav.pto.veilarbportefolje.mock;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.IdentOppslag;
import no.nav.common.health.HealthCheckResult;
import java.util.List;
import java.util.stream.Collectors;

public class AktorregisterClientMock implements AktorregisterClient {

    public static final String FNR = "10101010101";
    public static final String AKTOER_ID = "9999999999991";

    @Override
    public String hentFnr(String aktorId) {
        return FNR;
    }

    @Override
    public String hentAktorId(String fnr) {
        return AKTOER_ID;
    }

    @Override
    public List<IdentOppslag> hentFnr(List<String> list) {
        return list.stream()
                .map(aktorId -> new IdentOppslag(aktorId, aktorId + "fnr"))
                .collect(Collectors.toList());
    }

    @Override
    public List<IdentOppslag> hentAktorId(List<String> list) {
        return list.stream()
                .map(fnr -> new IdentOppslag(fnr, fnr + "aktorId"))
                .collect(Collectors.toList());
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy();
    }
}
