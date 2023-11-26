package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

import java.time.LocalDate;

public record HuskelappResponse(String huskelappId, Fnr brukerFnr, EnhetId enhetID, LocalDate frist, String kommentar, LocalDate endretDato, VeilederId endretAv) {
}
