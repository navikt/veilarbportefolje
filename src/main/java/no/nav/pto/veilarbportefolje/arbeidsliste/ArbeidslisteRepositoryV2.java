package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.ARBEIDSLISTE.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ArbeidslisteRepositoryV2 implements ArbeidslisteRepository {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public Optional<String> hentNavKontorForArbeidsliste(AktorId aktorId) {
        String sql = String.format("SELECT %s FROM %s WHERE %s=? ", NAV_KONTOR_FOR_ARBEIDSLISTE, TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getString(NAV_KONTOR_FOR_ARBEIDSLISTE), aktorId.get()))
        );
    }

    public Try<Arbeidsliste> retrieveArbeidsliste(AktorId aktorId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, AKTOERID);
        return Try.of(
                () -> queryForObjectOrNull(
                        () -> db.queryForObject(sql, this::arbeidslisteMapper, aktorId.get())
                )
        );
    }

    public Try<ArbeidslisteDTO> insertArbeidsliste(ArbeidslisteDTO dto) {
        return Try.of(
                () -> {
                    dto.setEndringstidspunkt(Timestamp.from(now()));
                    AktorId aktoerId = Optional
                            .ofNullable(dto.getAktorId())
                            .orElseThrow(() -> new RuntimeException("Fant ikke aktør-ID"));
                    dto.setAktorId(aktoerId);

                    upsert(aktoerId.get(), dto);
                    return dto;
                }
        ).onFailure(e -> log.warn("Kunne ikke inserte arbeidsliste til db", e));
    }


    public Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data) {
         final String updateSql = String.format(
                "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = ? WHERE %s = ?",
                TABLE_NAME, SIST_ENDRET_AV_VEILEDERIDENT, ENDRINGSTIDSPUNKT, OVERSKRIFT,
                KOMMENTAR, FRIST, KATEGORI, AKTOERID
        );

        Timestamp endringsTidspunkt = Timestamp.from(now());
        return Try.of(
                () -> {
                    int rows = db.update(updateSql, data.getVeilederId().getValue(), endringsTidspunkt, data.getOverskrift(),
                                    data.getKommentar(), data.getFrist(), data.getKategori().name(), data.getAktorId().get());

                    log.info("Oppdaterte arbeidsliste pa bruker {}, rader: {}", data.getAktorId().get(), rows);
                    return data.setEndringstidspunkt(endringsTidspunkt);
                }
        ).onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste i db", e));
    }

    public int slettArbeidsliste(AktorId aktoerId) {
        if (aktoerId == null) {
            return 0;
        }
        log.info("Sletter vedtak og utkast pa bruker: {}", aktoerId);
        return db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());
    }

    @SneakyThrows
    private Arbeidsliste arbeidslisteMapper(ResultSet rs, int row) {
        return new Arbeidsliste(
                VeilederId.of(rs.getString(SIST_ENDRET_AV_VEILEDERIDENT)),
                toZonedDateTime(rs.getTimestamp(ENDRINGSTIDSPUNKT)),
                rs.getString(OVERSKRIFT),
                rs.getString(KOMMENTAR),
                toZonedDateTime(rs.getTimestamp(FRIST)),
                Arbeidsliste.Kategori.valueOf(rs.getString(KATEGORI))
        );
    }

    private int upsert(String aktoerId, ArbeidslisteDTO dto){
        return db.update("INSERT INTO " + TABLE_NAME
                + " (" + AKTOERID +
                ", " + SIST_ENDRET_AV_VEILEDERIDENT +
                ", " + ENDRINGSTIDSPUNKT +
                ", " + OVERSKRIFT +
                ", " + KOMMENTAR +
                ", " + FRIST +
                ", " + KATEGORI +
                ", " + NAV_KONTOR_FOR_ARBEIDSLISTE + ") " +
                "VALUES(?,?,?,?,?,?,?,?) " +
                "ON CONFLICT (" + AKTOERID + ") DO UPDATE SET (" +
                    SIST_ENDRET_AV_VEILEDERIDENT +
                    ", " + ENDRINGSTIDSPUNKT +
                    ", " + OVERSKRIFT +
                    ", " + KOMMENTAR +
                    ", " + FRIST +
                    ", " + KATEGORI +
                    ", " + NAV_KONTOR_FOR_ARBEIDSLISTE + ")" +
                    " = (?,?,?,?,?,?,?)", aktoerId, dto.getVeilederId().getValue(), dto.getEndringstidspunkt(), dto.getOverskrift(), dto.getKommentar(), dto.getFrist(), dto.getKategori().toString(), dto.getNavKontorForArbeidsliste(),
                    dto.getVeilederId().getValue(), dto.getEndringstidspunkt(), dto.getOverskrift(), dto.getKommentar(), dto.getFrist(), dto.getKategori().toString(), dto.getNavKontorForArbeidsliste());
    }
}
