package no.nav.pto.veilarbportefolje.internal;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAllParallel;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalController {
    private final SelfTestChecks selfTestChecks;

    @GetMapping("/isReady")
    public void isReady() {
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