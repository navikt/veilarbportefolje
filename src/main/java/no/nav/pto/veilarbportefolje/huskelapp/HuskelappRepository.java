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
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.HUSKELAPP.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate;

@RequiredArgsConstructor
public class HuskelappRepository {
    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public UUID opprettHuskelapp(HuskelappOpprettRequest huskelappOpprettRequest, VeilederId veilederId) {
        UUID huskelappId = UUID.randomUUID();
        UUID endringsId = UUID.randomUUID();
        String sql = """
                INSERT INTO HUSKELAPP (
                    ENDRINGS_ID,
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
                    ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?
                )
                """;
        db.update(sql, endringsId, huskelappId, huskelappOpprettRequest.brukerFnr().get(), huskelappOpprettRequest.enhetId(), veilederId.getValue(), huskelappOpprettRequest.frist(), huskelappOpprettRequest.kommentar(), HuskelappStatus.AKTIV);
        return huskelappId;
    }

    @Transactional
    public UUID redigerHuskelapp(HuskelappRedigerRequest huskelappRedigerRequest, VeilederId veilederId) {
        UUID endringsId = UUID.randomUUID();

        String sqlSettForrigeRadInaktiv = """
                UPDATE HUSKELAPP SET STATUS = ? WHERE HUSKELAPP_ID = ? AND STATUS = ?
                """;
        db.update(sqlSettForrigeRadInaktiv, HuskelappStatus.IKKE_AKTIV, huskelappRedigerRequest.huskelappId(), HuskelappStatus.AKTIV);

        String sqlRedigerHuskelapp = """
                INSERT INTO HUSKELAPP (
                    ENDRINGS_ID,
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
                    ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?
                )
                """;
        db.update(sqlRedigerHuskelapp, endringsId, huskelappRedigerRequest.huskelappId(), huskelappRedigerRequest.brukerFnr().get(), huskelappRedigerRequest.enhetId(), veilederId.getValue(), huskelappRedigerRequest.frist(), huskelappRedigerRequest.kommentar(), HuskelappStatus.AKTIV);
        return endringsId;
    }

    public List<Huskelapp> hentHuskelapp(EnhetId enhetId, VeilederId veilederId) {
        return dbReadOnly.queryForList("""
                                SELECT hl.* FROM HUSKELAPP hl
                                INNER JOIN aktive_identer ai on ai.fnr = hl.fnr
                                INNER JOIN oppfolging_data o ON ai.aktorid = o.aktoerid
                                INNER JOIN oppfolgingsbruker_arena_v2 ob on ai.fnr = ob.fodselsnr
                                WHERE ob.nav_kontor = ?
                                AND o.veilederid = ?""",
                        enhetId.get(),
                        veilederId.getValue()
                )
                .stream()
                .map(HuskelappRepository::huskelappMapper)
                .toList();

    }

    public Optional<Huskelapp> hentHuskelapp(Fnr brukerFnr) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, FNR);
        return dbReadOnly.queryForList(sql, brukerFnr.get()).stream().map(HuskelappRepository::huskelappMapper).findFirst();
    }

    public Optional<Huskelapp> hentHuskelapp(UUID huskelappId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, HUSKELAPP_ID);
        return dbReadOnly.queryForList(sql, huskelappId).stream().map(HuskelappRepository::huskelappMapper).findFirst();
    }

    public void slettAlleHuskelappRaderPaaBruker(Fnr fnr) {
        String sql = String.format("DELETE FROM %s WHERE %s=? ", TABLE_NAME, FNR);
        db.update(sql, fnr);
    }

    public void settSisteHuskelappRadIkkeAktiv(UUID huskelappId) {
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ? AND STATUS = ?",
                TABLE_NAME, STATUS, HUSKELAPP_ID
        );
        db.update(sql, HuskelappStatus.IKKE_AKTIV, huskelappId, HuskelappStatus.AKTIV);
    }

    @SneakyThrows
    private static Huskelapp huskelappMapper(Map<String, Object> rs) {
        return new Huskelapp(
                UUID.fromString((String) rs.get(HUSKELAPP_ID)),
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
