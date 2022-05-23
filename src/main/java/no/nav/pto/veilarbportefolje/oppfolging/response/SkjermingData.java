package no.nav.pto.veilarbportefolje.oppfolging.response;

import lombok.Data;
import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;

@Data
public class SkjermingData {
    private final Fnr fnr;
    private final boolean er_skjermet;
    private final Timestamp skjermet_fra;
    private final Timestamp skjermet_til;

}

