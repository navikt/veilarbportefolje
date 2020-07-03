package no.nav.pto.veilarbportefolje.controller;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import no.nav.pto.veilarbportefolje.database.DatabaseConfig;
import no.nav.pto.veilarbportefolje.elastic.ElasticConfig;
import no.nav.pto.veilarbportefolje.krr.DigitalKontaktinformasjonConfig;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.FORVENTET_MINIMUM_ANTALL_DOKUMENTER;

@RestController
@RequestMapping("/internal")
public class InternalController {
    private List<SelfTestCheck> selftestChecks;

    @Autowired
    public InternalController(DigitalKontaktinformasjonV1 dkifV1, JdbcTemplate db) {
        this.selftestChecks = Arrays.asList(
                new SelfTestCheck(  String.format("Sjekker at antall dokumenter > %s", FORVENTET_MINIMUM_ANTALL_DOKUMENTER), false, ElasticConfig::checkHealth),
                new SelfTestCheck("Database for portefolje", true, () -> DatabaseConfig.dbPinger(db)),
                new SelfTestCheck("Ping av DKIF_V1. Henter reservasjon fra KRR.", false, ()-> DigitalKontaktinformasjonConfig.dkifV1Ping(dkifV1))
        );
    }

    @GetMapping("/isReady")
    public void isReady() {
        List<HealthCheck> healthChecks = Collections.emptyList();

        HealthCheckUtils.findFirstFailingCheck(healthChecks)
                .ifPresent((failedCheck) -> {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    @GetMapping("/isAlive")
    public void isAlive() {}

    @GetMapping("/selftest")
    public ResponseEntity selftest() {
        List<SelftTestCheckResult> checkResults = checkAllParallel(selftestChecks);
        String html = SelftestHtmlGenerator.generate(checkResults);
        int status = SelfTestUtils.findHttpStatusCode(checkResults, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
