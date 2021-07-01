package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.FodselsnummerUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OppfolginsbrukerRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public int leggTilEllerEndreOppfolgingsbruker(OppfolgingsbrukerKafkaDTO oppfolgingsbruker) {
        if (oppfolgingsbruker == null || oppfolgingsbruker.getAktoerid() == null) {
            return 0;
        }

        Optional<ZonedDateTime> sistEndretDato = getEndretDato(oppfolgingsbruker.getAktoerid());
        if (oppfolgingsbruker.getEndret_dato() == null || (sistEndretDato.isPresent() && sistEndretDato.get().isAfter(oppfolgingsbruker.getEndret_dato()))) {
            log.info("Oppdaterer ikke oppfolgingsbruker: {}", oppfolgingsbruker.getAktoerid());
            return 0;
        }
        return upsert(oppfolgingsbruker);
    }


    public Optional<OppfolgingsbrukerKafkaDTO> getOppfolgingsBruker(AktorId aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilOppfolgingsbruker, aktorId.get()))
        );
    }


    private Optional<ZonedDateTime> getEndretDato(String aktorId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s=? ", ENDRET_DATO, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilZonedDateTime, aktorId))
        );
    }

    @SneakyThrows
    private ZonedDateTime mapTilZonedDateTime(ResultSet rs, int row) {
        return toZonedDateTime(rs.getTimestamp(ENDRET_DATO));
    }

    @SneakyThrows
    private OppfolgingsbrukerKafkaDTO mapTilOppfolgingsbruker(ResultSet rs, int row) {
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

    private int upsert(OppfolgingsbrukerKafkaDTO oppfolgingsbruker) {
        java.sql.Date fodselsDato = toSqlDateOrNull(FodselsnummerUtils.lagFodselsdato(oppfolgingsbruker.getFodselsnr()));
        String kjonn = FodselsnummerUtils.lagKjonn(oppfolgingsbruker.getFodselsnr());

        boolean sperretAnsatt = oppfolgingsbruker.getSperret_ansatt() != null && oppfolgingsbruker.getSperret_ansatt();
        boolean er_doed = oppfolgingsbruker.getEr_doed() != null && oppfolgingsbruker.getEr_doed();
        boolean har_oppfolgingssak = oppfolgingsbruker.getHar_oppfolgingssak() != null && oppfolgingsbruker.getHar_oppfolgingssak();

        return db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTOERID + ", " + FODSELSNR + ", " + FORMIDLINGSGRUPPEKODE +
                        ", " + ISERV_FRA_DATO + ", " + ETTERNAVN + ", " + FORNAVN +
                        ", " + NAV_KONTOR + ", " + KVALIFISERINGSGRUPPEKODE + ", " + RETTIGHETSGRUPPEKODE +
                        ", " + HOVEDMAALKODE + ", " + SIKKERHETSTILTAK_TYPE_KODE + ", " + DISKRESJONSKODE +
                        ", " + HAR_OPPFOLGINGSSAK + ", " + SPERRET_ANSATT + ", " + ER_DOED +
                        ", " + DOED_FRA_DATO + ", " + ENDRET_DATO + ", " + KJONN +
                        ", " + FODSELS_DATO + ") " +
                        "VALUES(?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET " +
                        "(" + FODSELSNR + ", " + FORMIDLINGSGRUPPEKODE + ", " + ISERV_FRA_DATO +
                        ", " + ETTERNAVN + ", " + FORNAVN + ", " + NAV_KONTOR +
                        ", " + KVALIFISERINGSGRUPPEKODE + ", " + RETTIGHETSGRUPPEKODE + ", " + HOVEDMAALKODE +
                        ", " + SIKKERHETSTILTAK_TYPE_KODE + ", " + DISKRESJONSKODE + ", " + HAR_OPPFOLGINGSSAK +
                        ", " + SPERRET_ANSATT + ", " + ER_DOED + ", " + DOED_FRA_DATO +
                        ", " + ENDRET_DATO + ", " + KJONN + ", " + FODSELS_DATO +
                        ") = (?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?)",
                oppfolgingsbruker.getAktoerid(), oppfolgingsbruker.getFodselsnr(), oppfolgingsbruker.getFormidlingsgruppekode(),
                toTimestamp(oppfolgingsbruker.getIserv_fra_dato()), oppfolgingsbruker.getEtternavn(), oppfolgingsbruker.getFornavn(),
                oppfolgingsbruker.getNav_kontor(), oppfolgingsbruker.getKvalifiseringsgruppekode(), oppfolgingsbruker.getRettighetsgruppekode(),
                oppfolgingsbruker.getHovedmaalkode(), oppfolgingsbruker.getSikkerhetstiltak_type_kode(), oppfolgingsbruker.getFr_kode(),
                har_oppfolgingssak, sperretAnsatt, er_doed,
                toTimestamp(oppfolgingsbruker.getDoed_fra_dato()), toTimestamp(oppfolgingsbruker.getEndret_dato()), kjonn,
                fodselsDato,

                oppfolgingsbruker.getFodselsnr(), oppfolgingsbruker.getFormidlingsgruppekode(), toTimestamp(oppfolgingsbruker.getIserv_fra_dato()),
                oppfolgingsbruker.getEtternavn(), oppfolgingsbruker.getFornavn(), oppfolgingsbruker.getNav_kontor(),
                oppfolgingsbruker.getKvalifiseringsgruppekode(), oppfolgingsbruker.getRettighetsgruppekode(), oppfolgingsbruker.getHovedmaalkode(),
                oppfolgingsbruker.getSikkerhetstiltak_type_kode(), oppfolgingsbruker.getFr_kode(), har_oppfolgingssak,
                sperretAnsatt, er_doed, toTimestamp(oppfolgingsbruker.getDoed_fra_dato()),
                toTimestamp(oppfolgingsbruker.getEndret_dato()), kjonn, fodselsDato
        );
    }
}
