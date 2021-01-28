package no.nav.pto.veilarbportefolje.mal;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class MalEndringKafkaDTO {
    String aktorId;
    ZonedDateTime endretTidspunk;
    InnsenderData lagtInnAv;
    String veilederIdent;

    public enum InnsenderData {
        BRUKER,
        NAV
    }
}
