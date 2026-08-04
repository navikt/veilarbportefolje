package no.nav.pto.veilarbportefolje.skjerming;

import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;

public record SkjermingData(Fnr fnr, boolean er_skjermet, Timestamp skjermet_fra, Timestamp skjermet_til) {
}

