package no.nav.pto.veilarbportefolje.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static no.nav.common.utils.EnvironmentUtils.isProduction;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logger")
public class Frontendlogger {
    private final MetricsClient metricsClient;

    @PostMapping("/event")
    public void skrivEventTilInflux(FrontendEvent event) {
        Event toInflux = new Event(event.name + ".event");
        if (event.getTags() != null) {
            event.getTags().forEach(toInflux::addTagToReport);
        }
        if (event.getFields() != null) {
            event.getFields().forEach(toInflux::addFieldToReport);
        }
        toInflux.getTags().put("environment", isProduction().orElse(false) ? "p" : "q1");

        log.info("debug input: " + event);
        log.info("debug influx: " + toString(toInflux));
        metricsClient.report(toInflux);
    }

    @Data
    @Accessors(chain = true)
    static class FrontendEvent {
        private final String name;
        private final Map<String, Object> fields;
        private final Map<String, String> tags;

        @Override
        public String toString() {
            return "Name: " + name + ", fields: " + (fields == null ? null : fields.entrySet())
                    + ", tags: " + (tags == null ? null : tags.entrySet());
        }
    }

    public static String toString(Event event) {
        return "fields: " + (event.getFields() == null ? null : event.getFields().entrySet())
                + ", tags: " + (event.getTags() == null ? null : event.getTags().entrySet());
    }
}
