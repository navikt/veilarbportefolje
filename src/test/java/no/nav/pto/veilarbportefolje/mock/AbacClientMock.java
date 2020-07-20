package no.nav.pto.veilarbportefolje.mock;

import no.nav.common.abac.AbacClient;
import no.nav.common.abac.domain.request.XacmlRequest;
import no.nav.common.abac.domain.response.Decision;
import no.nav.common.abac.domain.response.Response;
import no.nav.common.abac.domain.response.XacmlResponse;
import no.nav.common.health.HealthCheckResult;

import java.util.Collections;

public class AbacClientMock implements AbacClient {

    @Override
    public String sendRawRequest(String s) {
        return "raw_abac_mock_request";
    }

    @Override
    public XacmlResponse sendRequest(XacmlRequest xacmlRequest) {
        XacmlResponse xacmlResponse = new XacmlResponse();
        xacmlResponse.withResponse(Collections.singletonList(new Response().withDecision(Decision.Permit)));
        return xacmlResponse;
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy();
    }
}
