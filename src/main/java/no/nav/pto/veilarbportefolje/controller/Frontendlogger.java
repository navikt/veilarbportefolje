package no.nav.pto.veilarbportefolje.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
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
    public void skrivEventTilInflux(FrontendEvent event) {
        Event toInflux = new Event(event.name);
        if (event.getTags() != null) {
            event.tags.forEach(toInflux::addTagToReport);
        }
        if (event.getFields() != null) {
            event.fields.forEach(toInflux::addFieldToReport);
        }
        metricsClient.report(toInflux);
    }

    @Data
    @Accessors(chain = true)
    static class FrontendEvent {
        private final String name;
        private final Map<String, Object> fields;
        private final Map<String, String> tags;
    }
}
