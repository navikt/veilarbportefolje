package no.nav.pto.veilarbportefolje.postgres.utils;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;

@Data
@Accessors(chain = true)
public class TiltakaktivitetEntity {
    String aktivitetId;
    AktorId aktoerId;
    String tiltakskode;
    String tiltaksnavn;
    ArenaDato tilDato;
    ArenaDato fraDato;
    Long version;
}
