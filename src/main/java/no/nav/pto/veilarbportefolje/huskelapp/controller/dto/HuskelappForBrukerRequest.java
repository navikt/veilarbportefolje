package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

public record HuskelappForBrukerRequest(Fnr fnr, EnhetId enhetId) {
}
