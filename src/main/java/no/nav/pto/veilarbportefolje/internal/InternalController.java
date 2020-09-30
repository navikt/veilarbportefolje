package no.nav.pto.veilarbportefolje.internal;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    public InternalController(SelfTestChecks selfTestChecks) {
        this.selfTestChecks = selfTestChecks;
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
}
