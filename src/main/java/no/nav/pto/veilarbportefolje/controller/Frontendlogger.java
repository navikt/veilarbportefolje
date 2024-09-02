package no.nav.pto.veilarbportefolje.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/logger")
@Tag(name = "Frontendlogger", description = "Frontendlogger-funksjonalitet")
public class Frontendlogger {

    @PostMapping("/event")
    @Operation(summary = "Skriv event til Influx", description = "Registrerer en frontend-hendelse og sender til InfluxDB.")
    public void skrivEventTilInflux(@RequestBody FrontendEvent event) {
        /*Event toInflux = new Event(event.name + ".event");
        if (event.getTags() != null) {
            event.getTags().forEach(toInflux::addTagToReport);
        }
        if (event.getFields() != null) {
            event.getFields().forEach(toInflux::addFieldToReport);
        }
        toInflux.getTags().put("environment", isProduction().orElse(false) ? "p" : "q1");

        if (!isProduction().orElse(false)) {
            secureLog.info("Skriver event til influx: " + eventToString(event.name, toInflux));
        }
        metricsClient.report(toInflux);*/
    }

    @Data
    @Accessors(chain = true)
    static class FrontendEvent {
        String name;
        Map<String, Object> fields;
        Map<String, String> tags;
    }

    public static String eventToString(String name, Event event) {
        return "name: " + name + ".event, fields: " + (event.getFields() == null ? null : event.getFields().entrySet())
                + ", tags: " + (event.getTags() == null ? null : event.getTags().entrySet());
    }
}
