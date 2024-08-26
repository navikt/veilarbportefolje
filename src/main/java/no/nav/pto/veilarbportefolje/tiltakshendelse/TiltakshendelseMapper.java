package no.nav.pto.veilarbportefolje.tiltakshendelse;

import lombok.SneakyThrows;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakshendelse;
import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.TILTAKSHENDELSE.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateTimeOrNull;

public class TiltakshendelseMapper {
    private TiltakshendelseMapper() { /* Unngår at vi har ein public constructor på klassen */}

    public static Tiltakshendelse tiltakshendelseMapper(Map<String, Object> rs) {
        return new Tiltakshendelse(
                (UUID) rs.get(ID),
                toLocalDateTimeOrNull((Timestamp) rs.get(OPPRETTET)),
                (String) rs.get(TEKST),
                (String) rs.get(LENKE),
                Tiltakstype.valueOf((String) rs.get(TILTAKSTYPE)),
                Fnr.of((String) rs.get(FNR))
        );
    }

    @SneakyThrows
    public static Tiltakshendelse tiltakshendelseMapper(ResultSet rs, int row) {
        return new Tiltakshendelse(
                (UUID) rs.getObject(ID),
                toLocalDateTimeOrNull(rs.getTimestamp(OPPRETTET)),
                rs.getString(TEKST),
                rs.getString(LENKE),
                Tiltakstype.valueOf(rs.getString(TILTAKSTYPE)),
                Fnr.of(rs.getString(FNR))
        );
    }
}
