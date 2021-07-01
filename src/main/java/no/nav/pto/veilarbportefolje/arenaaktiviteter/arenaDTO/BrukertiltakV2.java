package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class BrukertiltakV2 {
    private AktorId aktorId;
    private String tiltak;
    private Timestamp tildato;
}
