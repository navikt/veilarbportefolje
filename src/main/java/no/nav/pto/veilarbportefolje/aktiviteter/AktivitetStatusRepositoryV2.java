package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETTYPE_STATUS;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AktivitetStatusRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public Optional<AktivitetStatus> hentAktivitetTypeStatus(String aktorId, String type) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", AKTIVITETTYPE_STATUS.TABLE_NAME, AKTIVITETTYPE_STATUS.AKTOERID, AKTIVITETTYPE_STATUS.AKTIVITETTYPE);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapAktivitetTypeStatus, aktorId, type))
        );
    }

    private AktivitetStatus mapAktivitetTypeStatus(ResultSet rs, int row) throws SQLException {
        String aktorId = rs.getString(AKTIVITETTYPE_STATUS.AKTOERID);
        return new AktivitetStatus()
                .setAktoerid(aktorId == null ? null : AktorId.of(aktorId))
                .setAktivitetType(rs.getString(AKTIVITETTYPE_STATUS.AKTIVITETTYPE))
                .setAktiv(rs.getBoolean(AKTIVITETTYPE_STATUS.AKTIV))
                .setNesteStart(rs.getTimestamp(AKTIVITETTYPE_STATUS.NESTE_STARTDATO))
                .setNesteUtlop(rs.getTimestamp(AKTIVITETTYPE_STATUS.NESTE_UTLOPSDATO));
    }
}
