package no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.types.identer.AktorId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class GruppeAktivitetSchedueldDTO {
    String veiledningdeltakerId;
    String moteplanId;

    AktorId aktorId;
    Timestamp aktivitetperiodeFra;
    Timestamp aktivitetperiodeTil;
    long hendelseId;
    boolean aktiv;
}
