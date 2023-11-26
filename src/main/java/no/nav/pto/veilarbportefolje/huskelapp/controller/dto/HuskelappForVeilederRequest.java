package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

public record HuskelappForVeilederRequest(EnhetId enhetId, VeilederId veilederId) {
}
