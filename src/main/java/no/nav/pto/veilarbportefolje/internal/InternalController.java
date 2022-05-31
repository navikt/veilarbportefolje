package no.nav.pto.veilarbportefolje.internal;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final JdbcTemplate db;
    private final MetricsClient metricsClient;

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

    // Kjører hvert minutt
    @Scheduled(fixedRate = 60000)
    private void metrikkOppdatering() {
        String alleBrukereUnderOppfolgingHarLokaltLagretIdent = """
                select count(*) from oppfolging_data od
                    left join bruker_identer bi on bi.ident = od.aktoerid
                    where bi is null;
                """;
        var lokalIdent = db.queryForObject(alleBrukereUnderOppfolgingHarLokaltLagretIdent, Integer.class);

        Event event = new Event("portefolje.metrikker.usermapping");
        event.addFieldToReport("brukere", lokalIdent);
        metricsClient.report(event);
    }
}