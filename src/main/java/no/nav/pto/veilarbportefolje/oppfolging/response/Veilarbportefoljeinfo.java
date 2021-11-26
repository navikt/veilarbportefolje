package no.nav.pto.veilarbportefolje.oppfolging.response;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class Veilarbportefoljeinfo {
    private AktorId aktorId;

    private NavIdent veilederId;
    private boolean erUnderOppfolging;
    private boolean nyForVeileder;
    private boolean erManuell;
    private ZonedDateTime startDato;
}
