package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.PostgresTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.LEST_ARENA_HENDELSE_AKTIVITETER.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

@Slf4j
@Repository
public class ArenaHendelseRepository {
    private final JdbcTemplate db;

    @Autowired
    public ArenaHendelseRepository(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public int upsertAktivitetHendelse(String id, long hendelse) {
        log.info("Lagrer pa id: {}, ny hendelse: {}", id, hendelse);
        return db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTIVITETID + ", " + HENDELSE_ID + ") " +
                        "VALUES (?, ?) " +
                        "ON CONFLICT (" + AKTIVITETID + ") " +
                        "DO UPDATE SET " + HENDELSE_ID + " = ?",
                id, hendelse,
                hendelse
        );
    }

    public int upsertYtelsesHendelse(String id, long hendelse) {
        log.info("Lagrer pa id: {}, ny hendelse: {}", id, hendelse);
        return db.update("INSERT INTO " + PostgresTable.LEST_ARENA_HENDELSE_YTELSER.TABLE_NAME +
                        " (" + PostgresTable.LEST_ARENA_HENDELSE_YTELSER.VEDTAKID + ", " + PostgresTable.LEST_ARENA_HENDELSE_YTELSER.HENDELSE_ID + ") " +
                        "VALUES (?, ?) " +
                        "ON CONFLICT (" + PostgresTable.LEST_ARENA_HENDELSE_YTELSER.VEDTAKID + ") " +
                        "DO UPDATE SET " + PostgresTable.LEST_ARENA_HENDELSE_YTELSER.HENDELSE_ID + " = ?",
                id, hendelse,
                hendelse
        );
    }

    public Long retrieveAktivitetHendelse(String id) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", HENDELSE_ID, TABLE_NAME, AKTIVITETID);
        return queryForObjectOrNull(() -> db.queryForObject(sql, (ResultSet rs, int i) -> rs.getLong(HENDELSE_ID), id));
    }

    public Long retrieveYtelsesHendelse(String id) {
        String sql = String.format("SELECT %s FROM %s WHERE %s = ?", PostgresTable.LEST_ARENA_HENDELSE_YTELSER.HENDELSE_ID, PostgresTable.LEST_ARENA_HENDELSE_YTELSER.TABLE_NAME, PostgresTable.LEST_ARENA_HENDELSE_YTELSER.VEDTAKID);
        return queryForObjectOrNull(() -> db.queryForObject(sql, (ResultSet rs, int i) -> rs.getLong(PostgresTable.LEST_ARENA_HENDELSE_YTELSER.HENDELSE_ID), id));
    }

}
