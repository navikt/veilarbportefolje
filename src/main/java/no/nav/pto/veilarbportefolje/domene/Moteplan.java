package no.nav.pto.veilarbportefolje.domene;

import java.time.ZonedDateTime;

/** Møteplandata som hentes ut fra database. */
public record Moteplan(Motedeltaker deltaker, String dato, ZonedDateTime starttidspunkt, ZonedDateTime sluttidspunkt, boolean avtaltMedNav) {
}
