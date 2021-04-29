package no.nav.pto.veilarbportefolje.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.safeNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Data
@Accessors(chain = true)
public class Dialogdata {
    public String aktorId;
    public ZonedDateTime sisteEndring;
    public ZonedDateTime tidspunktEldsteVentende;
    public ZonedDateTime tidspunktEldsteUbehandlede;

    public String toSqlInsertString() {
        return safeNull(getAktorId()) + ", " +
                safeNull(toTimestamp(getSisteEndring())) + ",  " +
                safeNull(toTimestamp(getTidspunktEldsteVentende())) + ", " +
                safeNull(toTimestamp(getTidspunktEldsteUbehandlede()));

    }

    public String toSqlUpdateString() {
        return safeNull(toTimestamp(getSisteEndring())) + ", " +
                safeNull(toTimestamp(getTidspunktEldsteVentende())) + ", " +
                safeNull(toTimestamp(getTidspunktEldsteUbehandlede()));
    }
}
