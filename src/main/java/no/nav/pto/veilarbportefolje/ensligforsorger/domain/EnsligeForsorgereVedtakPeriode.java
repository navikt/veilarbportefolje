package no.nav.pto.veilarbportefolje.ensligforsorger.domain;

import java.time.LocalDate;

public record EnsligeForsorgereVedtakPeriode(long vedtakId, LocalDate periode_fra, LocalDate periode_til,
                                             Integer periodetype, Integer aktivitetstype) {
}
