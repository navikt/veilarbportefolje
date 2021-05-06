package no.nav.pto.veilarbportefolje.cv.dto;

import lombok.Data;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

@Data
public class CVMelding {
    AktorId aktoerId;
    Fnr fnr;
    MeldingType meldingType;
    Ressurs ressurs;
}
