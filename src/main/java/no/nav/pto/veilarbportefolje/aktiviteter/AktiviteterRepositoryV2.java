package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.AKTIVITETID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.AKTIVITETTYPE;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.AVTALT;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.FRADATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.STATUS;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.TILDATO;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.VERSION;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AktiviteterRepositoryV2 {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    @Transactional
    public boolean tryLagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        try {
            if (aktivitet.isHistorisk()) {
                deleteById(aktivitet.getAktivitetId());
                return true;
            } else if (erNyVersjonAvAktivitet(aktivitet)) {
                upsertAktivitet(aktivitet);
                return true;
            }
        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata fra topic for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
        }
        return false;
    }

    public void upsertAktivitet(KafkaAktivitetMelding aktivitet) {
        db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTIVITETID + ", " + AKTOERID + ", " + AKTIVITETTYPE + ", " + AVTALT + ", " + FRADATO + ", " + TILDATO + ", " + STATUS + ", " + VERSION + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTIVITETID + ") " +
                        "DO UPDATE SET (" + AKTOERID + ", " + AKTIVITETTYPE + ", " + AVTALT + ", " + FRADATO + ", " + TILDATO + ", " + STATUS + ", " + VERSION + ") = (?, ?, ?, ?, ?, ?, ?)",
                aktivitet.getAktivitetId(),
                aktivitet.getAktorId(), aktivitet.getAktivitetType().name().toLowerCase(), aktivitet.isAvtalt(), toTimestamp(aktivitet.getFraDato()), toTimestamp(aktivitet.getTilDato()), aktivitet.getAktivitetStatus().name().toLowerCase(), aktivitet.getVersion(),
                aktivitet.getAktorId(), aktivitet.getAktivitetType().name().toLowerCase(), aktivitet.isAvtalt(), toTimestamp(aktivitet.getFraDato()), toTimestamp(aktivitet.getTilDato()), aktivitet.getAktivitetStatus().name().toLowerCase(), aktivitet.getVersion()
        );
    }

    private boolean erNyVersjonAvAktivitet(KafkaAktivitetMelding aktivitet) {
        Long kommendeVersjon = aktivitet.getVersion();
        if (kommendeVersjon == null) {
            return false;
        }
        Long databaseVersjon = getVersjon(aktivitet.getAktivitetId());
        if (databaseVersjon == null) {
            return true;
        }
        return kommendeVersjon.compareTo(databaseVersjon) > 0;
    }

    private Long getVersjon(String aktivitetId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTIVITETID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getLong(VERSION), aktivitetId))
        ).orElse(-1L);
    }

    public AktoerAktiviteter getAktiviteterForAktoerid(AktorId aktoerid, boolean brukIkkeAvtalteAktiviteter) {
        String sql;
        if (brukIkkeAvtalteAktiviteter) {
            sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        } else {
            sql = String.format("SELECT * FROM %s WHERE %s = ? AND %s", TABLE_NAME, AKTOERID, AVTALT);
        }

        List<AktivitetDTO> aktiviteter = Optional.ofNullable(
                queryForObjectOrNull(() -> db.query(sql, this::mapToAktivitetDTOList, aktoerid.get()))
        ).orElse(new ArrayList<>());

        return new AktoerAktiviteter(aktoerid.get()).setAktiviteter(aktiviteter);
    }

    public void deleteById(String aktivitetid) {
        log.info("Sletter alle aktiviteter med id {}", aktivitetid);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTIVITETID), aktivitetid);
    }

    public List<AktivitetDTO> getPasserteAktiveUtdanningsAktiviter() {
        final String sql = "SELECT * FROM aktiviteter WHERE NOT status = 'fullfort' AND date_trunc('day', tildato) < date_trunc('day', current_timestamp)";

       return Optional.ofNullable(
                queryForObjectOrNull(() -> db.query(sql, this::mapToAktivitetDTOList))
        ).orElse(new ArrayList<>());
    }

    public void setTilFullfort(String aktivitetid) {
        log.info("Setter status flagget til aktivitet: {}, til verdien fullfort", aktivitetid);
        db.update("UPDATE aktiviteter SET status = 'fullfort' WHERE aktivitetid = ?", aktivitetid);
    }

    @SneakyThrows
    private List<AktivitetDTO> mapToAktivitetDTOList(ResultSet rs) {
        List<AktivitetDTO> aktiviteter = new ArrayList<>();
        while (rs.next()) {
            aktiviteter.add(new AktivitetDTO()
                    .setAktivitetID(rs.getString(AKTIVITETID))
                    .setAktivitetType(rs.getString(AKTIVITETTYPE))
                    .setStatus(rs.getString(STATUS))
                    .setFraDato(rs.getTimestamp(FRADATO))
                    .setTilDato(rs.getTimestamp(TILDATO)));
        }
        return aktiviteter;
    }
}
