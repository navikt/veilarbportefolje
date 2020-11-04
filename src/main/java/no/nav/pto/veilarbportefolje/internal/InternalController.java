package no.nav.pto.veilarbportefolje.internal;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import no.nav.common.metrics.InfluxClient;
import no.nav.pto.veilarbportefolje.metrikker.MetricsUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

@RestController
@RequestMapping("/internal")
public class InternalController {

    private final SelfTestChecks selfTestChecks;
    private final JdbcTemplate db;

    @Autowired
    public InternalController(SelfTestChecks selfTestChecks, JdbcTemplate db) {
        this.selfTestChecks = selfTestChecks;
        this.db = db;
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
    public void isAlive() {
    }

    @GetMapping("/selftest")
    public ResponseEntity selftest() {
        List<SelftTestCheckResult> results = checkAllParallel(selfTestChecks.getSelfTestChecks());
        String html = SelftestHtmlGenerator.generate(results);
        int status = SelfTestUtils.findHttpStatusCode(results, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    // kl 1:00 hver dag
    @Scheduled(cron = "0 * 1 * * *")
    private void metrikkOppdatering() {
        String sql =  "SELECT count(*) FROM AKTOERID_TO_PERSONID "
                    + "WHERE PERSONID IN "
                    + "(SELECT PERSONID FROM AKTOERID_TO_PERSONID GROUP BY PERSONID HAVING max(GJELDENE) = 0)";
        MetricsUtils.timed("portefolje.metrikker.usermapping", () -> {
            db.queryForObject(sql, Integer.class);
        }, new InfluxClient());
    }
}