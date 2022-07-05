package no.nav.pto.veilarbportefolje.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.metrics.MetricsClient;
import org.joda.time.Instant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logger")
public class Frontendlogger {
    private final MetricsClient metricsClient;

    @PostMapping("/event")
    public void skrivEventTilInflux(FrontendEvent event){
        metricsClient.report(event.name, event.fields, event.tags, Instant.now().getMillis());
    }

    @Data
    @Accessors(chain = true)
    static class FrontendEvent {
        private final String name;
        private final Map<String, Object> fields;
        private final Map<String, String> tags;
    }
}
