package no.nav.pto.veilarbportefolje.aktiviteter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.AKTIVITETER.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AktiviteterRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    @Transactional
    public void tryLagreAktivitetData(KafkaAktivitetMelding aktivitet) {
        try {
            if (aktivitet.isHistorisk()) {
                deleteById(aktivitet.getAktivitetId());
            } else if (erNyVersjonAvAktivitet(aktivitet)) {
                upsertAktivitet(aktivitet);
            }
        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata fra topic for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
        }
    }

    private void upsertAktivitet(KafkaAktivitetMelding aktivitet) {
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

    public AktoerAktiviteter getAvtalteAktiviteterForAktoerid(AktorId aktoerid) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? AND %s", TABLE_NAME, AKTOERID, AVTALT);

        List<AktivitetDTO> aktiviteter = Optional.ofNullable(
                queryForObjectOrNull(() -> db.query(sql, this::mapToAktivitetDTOList, aktoerid))
        ).orElse(new ArrayList<>());

        return new AktoerAktiviteter(aktoerid.get()).setAktiviteter(aktiviteter);
    }

    public void deleteById(String aktivitetid) {
        log.info("Sletter alle aktiviteter med id {}", aktivitetid);
        db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktivitetid);
    }

    private List<AktivitetDTO> mapToAktivitetDTOList(ResultSet rs) throws SQLException {
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

    public void setAvtalt(String aktivitetid, boolean avtalt) {
        log.info("Setter avtalt flagget til aktivitet: {}, til verdien: {}", aktivitetid, avtalt);
        db.update(String.format("UPDATE %s SET %s = ? WHERE %s = ?", TABLE_NAME, AVTALT, AKTIVITETID), avtalt, aktivitetid);
    }

    public AktivitetStatus getAktivitetStatus(AktorId aktoerid, KafkaAktivitetMelding.AktivitetTypeData aktivitetType) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String sql = String.format("SELECT * FROM %s WHERE %s = ? AND %s = ? AND %s", TABLE_NAME, AKTOERID, AKTIVITETTYPE, AVTALT);

        List<AktivitetDTO> aktiveAktiviteter = Optional.ofNullable(
                        queryForObjectOrNull(() -> db.query(sql, this::mapToAktivitetDTOList, aktoerid, aktivitetType.name()))
                ).orElse(new ArrayList<>()).stream()
                .filter(AktivitetUtils::harIkkeStatusFullfort)
                .collect(Collectors.toList());

        Timestamp nesteStart = aktiveAktiviteter.stream()
                .map(AktivitetDTO::getFraDato)
                .filter(Objects::nonNull)
                .filter(startDato -> startDato.toLocalDateTime().toLocalDate().isAfter(yesterday))
                .min(Comparator.naturalOrder())
                .orElse(null);
        Timestamp nesteUtlopsdato = aktiveAktiviteter.stream()
                .map(AktivitetDTO::getTilDato)
                .filter(Objects::nonNull)
                .filter(utlopsDato -> utlopsDato.toLocalDateTime().toLocalDate().isAfter(yesterday))
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new AktivitetStatus()
                .setAktoerid(aktoerid)
                .setAktiv(!aktiveAktiviteter.isEmpty())
                .setNesteStart(nesteStart)
                .setNesteUtlop(nesteUtlopsdato);
    }
}
