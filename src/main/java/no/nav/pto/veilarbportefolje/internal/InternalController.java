package no.nav.pto.veilarbportefolje.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Internal", description = "Intern funksjonalitet. Benyttes hovedsakelig av kjøretidsplattformen.")
public class InternalController {
    private final SelfTestChecks selfTestChecks;

    @GetMapping("/isReady")
    @Operation(summary = "Applikasjon klar for trafikk", description = "Sjekker om applikasjonen er klar til å motta trafikk.")
    public void isReady() {
    }

    @GetMapping("/isAlive")
    @Operation(summary = "Applikasjon kjører", description = "Sjekker om applikasjonen kjører.")
    public void isAlive() {
    }

    @GetMapping("/selftest")
    @Operation(summary = "Sjekk applikasjonens helsesjekker", description = "Utfører helsesjekk for tjenester som applikasjonen er avhengig av og generer rapport.")
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