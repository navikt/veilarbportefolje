package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETTYPE_STATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITET_STATUS;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AktivitetStatusRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public void upsertAktivitetTypeStatus(AktivitetStatus status, String type) {
        if (status.getAktoerid() == null || type == null) {
            log.warn("Kunne ikke lagre aktivitet pga. null verdier");
            return;
        }

        db.update("INSERT INTO " + AKTIVITETTYPE_STATUS.TABLE_NAME +
                        " (" + AKTIVITETTYPE_STATUS.AKTOERID + ", " + AKTIVITETTYPE_STATUS.AKTIVITETTYPE + ", " + AKTIVITETTYPE_STATUS.AKTIV + ", " + AKTIVITETTYPE_STATUS.NESTE_UTLOPSDATO + ", " + AKTIVITETTYPE_STATUS.NESTE_STARTDATO + ") " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTIVITETTYPE_STATUS.AKTOERID + ", " + AKTIVITETTYPE_STATUS.AKTIVITETTYPE + ") " +
                        "DO UPDATE SET (" + AKTIVITETTYPE_STATUS.AKTIV + ", " + AKTIVITETTYPE_STATUS.NESTE_UTLOPSDATO + ", " + AKTIVITETTYPE_STATUS.NESTE_STARTDATO + ") = (?, ?, ?)",
                status.getAktoerid().get(), type.toLowerCase(),
                status.aktiv, status.getNesteUtlop(), status.getNesteStart(),
                status.aktiv, status.getNesteUtlop(), status.getNesteStart()
        );
    }

    public void upsertAktivitetStatus(Brukerdata status) {
        if (status.getAktoerid() == null) {
            log.warn("Kunne ikke lagre aktivitet status pga. null verdier");
            return;
        }

        db.update("INSERT INTO " + AKTIVITET_STATUS.TABLE_NAME +
                        " (" + AKTIVITET_STATUS.AKTOERID + ", " + AKTIVITET_STATUS.NYESTEUTLOPTEAKTIVITET + ", " + AKTIVITET_STATUS.FORRIGE_AKTIVITET_START + ", " + AKTIVITET_STATUS.AKTIVITET_START + ", " + AKTIVITET_STATUS.NESTE_AKTIVITET_START + ") " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTIVITET_STATUS.AKTOERID + ") " +
                        "DO UPDATE SET (" + AKTIVITET_STATUS.NYESTEUTLOPTEAKTIVITET + ", " + AKTIVITET_STATUS.FORRIGE_AKTIVITET_START + ", " + AKTIVITET_STATUS.AKTIVITET_START + ", " + AKTIVITET_STATUS.NESTE_AKTIVITET_START + ") = (?, ?, ?, ?)",
                status.getAktoerid(),
                status.getNyesteUtlopteAktivitet(), status.getForrigeAktivitetStart(), status.getAktivitetStart(), status.getNesteAktivitetStart(),
                status.getNyesteUtlopteAktivitet(), status.getForrigeAktivitetStart(), status.getAktivitetStart(), status.getNesteAktivitetStart()
        );
    }


    public Optional<AktivitetStatus> hentAktivitetTypeStatus(String aktorId, String type) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ?", AKTIVITETTYPE_STATUS.TABLE_NAME, AKTIVITETTYPE_STATUS.AKTOERID, AKTIVITETTYPE_STATUS.AKTIVITETTYPE);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapAktivitetStatus, aktorId, type))
        );
    }

    private AktivitetStatus mapAktivitetStatus(ResultSet rs, int row) throws SQLException {
        String aktorId = rs.getString(AKTIVITETTYPE_STATUS.AKTOERID);
        return new AktivitetStatus()
                .setAktoerid(aktorId == null ? null : AktorId.of(aktorId))
                .setAktivitetType(rs.getString(AKTIVITETTYPE_STATUS.AKTIVITETTYPE))
                .setAktiv(rs.getBoolean(AKTIVITETTYPE_STATUS.AKTIV))
                .setNesteStart(rs.getTimestamp(AKTIVITETTYPE_STATUS.NESTE_STARTDATO))
                .setNesteUtlop(rs.getTimestamp(AKTIVITETTYPE_STATUS.NESTE_UTLOPSDATO));
    }
}
