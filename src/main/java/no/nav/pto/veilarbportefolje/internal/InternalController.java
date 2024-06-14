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
@Tag(name = "Internal", description = "Intern funksjonalitet. Benyttes hovedsaklig av kjøretidsplattformen.")
public class InternalController {
    private final SelfTestChecks selfTestChecks;

    @Operation(summary = "Applikasjon klar for trafikk", description = "Sjekk om applikasjonen er klar til å motta trafikk.")
    @GetMapping("/isReady")
    public void isReady() {
    }

    @Operation(summary = "Applikasjon kjører", description = "Sjekk om applikasjonen kjører.")
    @GetMapping("/isAlive")
    public void isAlive() {
    }

    @Operation(summary = "Sjekk applikasjonens helsesjekker", description = "Utfør helsesjekk for tjenester applikasjonen er avhengig av og generer rapport.")
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