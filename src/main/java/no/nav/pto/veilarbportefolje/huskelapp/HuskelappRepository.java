package no.nav.pto.veilarbportefolje.huskelapp;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.huskelapp.domain.HuskelappStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.HUSKELAPP.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@RequiredArgsConstructor
@Repository
public class HuskelappRepository {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public UUID opprettHuskelapp(HuskelappOpprettRequest huskelappOpprettRequest, VeilederId veilederId) {
        UUID huskelappId = UUID.randomUUID();
        String sql = """
                INSERT INTO HUSKELAPP (
                    HUSKELAPP_ID,
                    FNR,
                    ENHET_ID,
                    ENDRET_AV_VEILEDER,
                    ENDRET_DATO,
                    FRIST,
                    KOMMENTAR,
                    STATUS
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;
        db.update(sql, huskelappId, huskelappOpprettRequest.brukerFnr().get(), huskelappOpprettRequest.enhetId().get(), veilederId.getValue(), Timestamp.from(Instant.now()), toTimestamp(huskelappOpprettRequest.frist()), huskelappOpprettRequest.kommentar(), HuskelappStatus.AKTIV.name());
        return huskelappId;
    }

    @Transactional
    public void redigerHuskelapp(HuskelappRedigerRequest huskelappRedigerRequest, VeilederId veilederId) {
        settSisteHuskelappRadIkkeAktiv(huskelappRedigerRequest.huskelappId());

        String sqlRedigerHuskelapp = """
                INSERT INTO HUSKELAPP (
                    HUSKELAPP_ID,
                    FNR,
                    ENHET_ID,
                    ENDRET_AV_VEILEDER,
                    ENDRET_DATO,
                    FRIST,
                    KOMMENTAR,
                    STATUS
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;
        db.update(sqlRedigerHuskelapp, huskelappRedigerRequest.huskelappId(), huskelappRedigerRequest.brukerFnr().get(), huskelappRedigerRequest.enhetId().get(), veilederId.getValue(), Timestamp.from(Instant.now()), toTimestamp(huskelappRedigerRequest.frist()), huskelappRedigerRequest.kommentar(), HuskelappStatus.AKTIV.name());
    }

    public List<Huskelapp> hentAktivHuskelapp(EnhetId enhetId, VeilederId veilederId) {
        return dbReadOnly.queryForList("""
                                SELECT hl.* FROM HUSKELAPP hl
                                INNER JOIN aktive_identer ai on ai.fnr = hl.fnr
                                INNER JOIN oppfolging_data o ON ai.aktorid = o.aktoerid
                                INNER JOIN oppfolgingsbruker_arena_v2 ob on ai.fnr = ob.fodselsnr
                                WHERE ob.nav_kontor = ?
                                AND o.veilederid = ?
                                AND hl.status = ?""",
                        enhetId.get(),
                        veilederId.getValue(),
                        HuskelappStatus.AKTIV.name()
                )
                .stream()
                .map(HuskelappRepository::huskelappMapper)
                .toList();

    }

    public Optional<Huskelapp> hentAktivHuskelapp(Fnr brukerFnr) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? AND STATUS = ?", TABLE_NAME, FNR);
        return dbReadOnly.queryForList(sql, brukerFnr.get(), HuskelappStatus.AKTIV.name()).stream().map(HuskelappRepository::huskelappMapper).findFirst();
    }

    public Optional<Huskelapp> hentAktivHuskelapp(UUID huskelappId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, HUSKELAPP_ID);
        return dbReadOnly.queryForList(sql, huskelappId).stream().map(HuskelappRepository::huskelappMapper).findFirst();
    }

    public List<Huskelapp> hentAlleRaderPaHuskelapp(UUID huskelappId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, HUSKELAPP_ID);
        return dbReadOnly.queryForList(sql, huskelappId).stream().map(HuskelappRepository::huskelappMapper).toList();
    }

    public void slettAlleHuskelappRaderPaaBruker(Fnr fnr) {
        String sql = String.format("DELETE FROM %s WHERE %s=? ", TABLE_NAME, FNR);
        db.update(sql, fnr.get());
    }

    public void deaktivereAlleHuskelappRaderPaaBruker(Fnr fnr) {
        String sql = String.format("UPDATE %s SET %s = ? WHERE %s = ? AND %s = ? ", TABLE_NAME, STATUS, FNR, STATUS);
        db.update(sql, HuskelappStatus.IKKE_AKTIV.name(), fnr.get(), HuskelappStatus.AKTIV.name());
    }

    public void settSisteHuskelappRadIkkeAktiv(UUID huskelappId) {
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND STATUS = ?",
                TABLE_NAME, STATUS, HUSKELAPP_ID
        );
        db.update(sql, HuskelappStatus.IKKE_AKTIV.name(), huskelappId, HuskelappStatus.AKTIV.name());
    }

    public Optional<String> hentNavkontorPaHuskelapp(Fnr brukerFnr) {
        //Hvordan gjør vi dette ved støtte for flere huskelapper...
        String sql = String.format("SELECT ENHET_ID FROM %s WHERE %s=? AND STATUS = ?", TABLE_NAME, FNR);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getString(ENHET_ID), brukerFnr.get(), HuskelappStatus.AKTIV.name())));
    }

    @SneakyThrows
    private static Huskelapp huskelappMapper(Map<String, Object> rs) {
        return new Huskelapp(
                (UUID) rs.get(HUSKELAPP_ID),
                Fnr.of((String) rs.get(FNR)),
                EnhetId.of((String) rs.get(ENHET_ID)),
                VeilederId.of((String) rs.get(ENDRET_AV_VEILEDER)),
                toLocalDate((Timestamp) rs.get(ENDRET_DATO)),
                toLocalDate((Timestamp) rs.get(FRIST)),
                (String) rs.get(KOMMENTAR),
                huskelappStatusMapper((String) rs.get(STATUS))
        );
    }

    @SneakyThrows
    private static HuskelappStatus huskelappStatusMapper(String status) {
        return switch (status) {
            case "AKTIV" -> HuskelappStatus.AKTIV;
            case "IKKE_AKTIV" -> HuskelappStatus.IKKE_AKTIV;
            default -> null;
        };
    }
}
