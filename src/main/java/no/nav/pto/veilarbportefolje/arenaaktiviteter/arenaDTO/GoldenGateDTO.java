package no.nav.pto.veilarbportefolje.arenaaktiviteter.arenaDTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public abstract class GoldenGateDTO <T extends ArenaInnholdKafka> {
    String table;
    @JsonAlias("op_type") String operationType;
    @JsonAlias("op_ts") String operationTimestamp;
    @JsonAlias("current_ts") String currentTimestamp;
    String pos;

    public abstract T getAfter();
    public abstract T getBefore();
}

