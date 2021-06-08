package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public abstract class GoldenGateDTO {
    String table;
    @JsonAlias("op_type") String operationType;
    @JsonAlias("op_ts") String operationTimestamp;
    @JsonAlias("current_ts") String currentTimestamp;
    String pos;

    public abstract ArenaInnholdKafka getAfter();
    public abstract ArenaInnholdKafka getBefore();
}

