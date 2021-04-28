package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.*;

@Slf4j
@Repository
public class OppfolginsbrukerRepositoryV2 {
    private final JdbcTemplate db;

    @Autowired
    public OppfolginsbrukerRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public int LeggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerKafkaDTO oppfolgingsbruker) {
        if (oppfolgingsbruker == null || oppfolgingsbruker.getAktoerid() == null) {
            return 0;
        }

        Optional<Timestamp> sistEndretDato = getEndretDato(oppfolgingsbruker.getAktoerid());
        if (oppfolgingsbruker.getEndret_dato() == null || (sistEndretDato.isPresent() && sistEndretDato.get().toInstant().isAfter(oppfolgingsbruker.getEndret_dato().toInstant()))) {
            log.info("Oppdaterer ikke oppfolgingsbruker: {}", oppfolgingsbruker.getAktoerid());
            return 0;
        }

        return db.update("INSERT INTO " + TABLE_NAME
                + " (" + SQLINSERT_STRING + ") " +
                "VALUES(" + oppfolgingsbruker.toSqlInsertString() + ") " +
                "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET (" + SQLUPDATE_STRING + ") = (" + oppfolgingsbruker.toSqlUpdateString() + ")");
    }


    public Optional<OppfolgingsbrukerKafkaDTO> getOppfolgingsBruker(AktorId aktorId) {
        final OppfolgingsbrukerKafkaDTO oppfolgingsbruker = SqlUtils.select(db, TABLE_NAME, this::mapTilOppfolgingsbruker)
                .column("*")
                .where(WhereClause.equals(AKTOERID, aktorId.get()))
                .execute();

        return Optional.ofNullable(oppfolgingsbruker);
    }

    private Optional<Timestamp> getEndretDato(String aktorId) {
        return Optional.ofNullable(
                db.queryForObject("SELECT * FROM OPPFOLGINGSBRUKER_ARENA WHERE AKTOERID = " + aktorId, Timestamp.class)
        );
    }

    @SneakyThrows
    private OppfolgingsbrukerKafkaDTO mapTilOppfolgingsbruker(ResultSet rs) {
        if (rs == null || rs.getString(AKTOERID) == null) {
            return null;
        }
        return new OppfolgingsbrukerKafkaDTO()
                .setAktoerid(rs.getString(AKTOERID))
                .setFodselsnr(rs.getString(FODSELSNR))
                .setFormidlingsgruppekode(rs.getString(FORMIDLINGSGRUPPEKODE))
                .setIserv_fra_dato(toZonedDateTime(rs.getTimestamp(ISERV_FRA_DATO)))
                .setEtternavn(rs.getString(ETTERNAVN))
                .setFornavn(rs.getString(FORNAVN))
                .setNav_kontor(rs.getString(NAV_KONTOR))
                .setKvalifiseringsgruppekode(rs.getString(KVALIFISERINGSGRUPPEKODE))
                .setRettighetsgruppekode(rs.getString(RETTIGHETSGRUPPEKODE))
                .setHovedmaalkode(rs.getString(HOVEDMAALKODE))
                .setSikkerhetstiltak_type_kode(rs.getString(SIKKERHETSTILTAK_TYPE_KODE))
                .setFr_kode(rs.getString(DISKRESJONSKODE))
                .setHar_oppfolgingssak(rs.getBoolean(HAR_OPPFOLGINGSSAK))
                .setSperret_ansatt(rs.getBoolean(SPERRET_ANSATT))
                .setEr_doed(rs.getBoolean(ER_DOED))
                .setDoed_fra_dato(toZonedDateTime(rs.getTimestamp(DOED_FRA_DATO)))
                .setEndret_dato(toZonedDateTime(rs.getTimestamp(ENDRET_DATO)));
    }
}
