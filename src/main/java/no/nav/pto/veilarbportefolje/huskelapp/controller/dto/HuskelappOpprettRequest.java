package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappOpprettRequest(Fnr brukerFnr, LocalDate frist, String kommentar, EnhetId enhetId) {
}
