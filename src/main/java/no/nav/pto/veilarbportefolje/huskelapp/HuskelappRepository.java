package no.nav.pto.veilarbportefolje.huskelapp;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOutputDto;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.HuskelappStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.HUSKELAPP.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

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
                    ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;
        db.update(sql, endringsId, huskelappId, huskelappOpprettRequest.brukerFnr().get(), huskelappOpprettRequest.enhetId().get(), veilederId.getValue(), Timestamp.from(Instant.now()), huskelappOpprettRequest.frist(), huskelappOpprettRequest.kommentar(), HuskelappStatus.AKTIV.name());
        return huskelappId;
    }

    @Transactional
    public UUID redigerHuskelapp(HuskelappRedigerRequest huskelappRedigerRequest, VeilederId veilederId) {
        UUID endringsId = UUID.randomUUID();

        String sqlSettForrigeRadInaktiv = """
                UPDATE HUSKELAPP SET STATUS = ? WHERE HUSKELAPP_ID = ? AND STATUS = ?
                """;
        db.update(sqlSettForrigeRadInaktiv, HuskelappStatus.IKKE_AKTIV.name(), huskelappRedigerRequest.huskelappId(), HuskelappStatus.AKTIV.name());

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
                    ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """;
        db.update(sqlRedigerHuskelapp, endringsId, huskelappRedigerRequest.huskelappId(), huskelappRedigerRequest.brukerFnr().get(), huskelappRedigerRequest.enhetId().get(), veilederId.getValue(), Timestamp.from(Instant.now()), huskelappRedigerRequest.frist(), huskelappRedigerRequest.kommentar(), HuskelappStatus.AKTIV.name());
        return endringsId;
    }

    public List<HuskelappOutputDto> hentHuskelapp(EnhetId enhetId, VeilederId veilederId) {
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
                .map(HuskelappRepository::huskelappOutputListMapper)
                .toList();

    }

    public Optional<HuskelappOutputDto> hentHuskelapp(Fnr brukerFnr) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? AND STATUS = ?", TABLE_NAME, FNR);
        return dbReadOnly.queryForList(sql, brukerFnr.get(), HuskelappStatus.AKTIV.name()).stream().map(HuskelappRepository::huskelappOutputListMapper).findFirst();
    }

    public Optional<HuskelappOutputDto> hentHuskelapp(UUID huskelappId) {
        String sql = String.format("SELECT * FROM %s WHERE %s=? ", TABLE_NAME, HUSKELAPP_ID);
        return dbReadOnly.queryForList(sql, huskelappId).stream().map(HuskelappRepository::huskelappOutputListMapper).findFirst();
    }

    public void slettAlleHuskelappRaderPaaBruker(Fnr fnr) {
        String sql = String.format("DELETE FROM %s WHERE %s=? ", TABLE_NAME, FNR);
        db.update(sql, fnr);
    }

    public void settSisteHuskelappRadIkkeAktiv(UUID huskelappId) {
        //TODO: Må vi også si WHERE STATUS = AKTIV
        String sql = String.format(
                "UPDATE %s SET %s = ? WHERE %s = ?",
                TABLE_NAME, STATUS, HUSKELAPP_ID
        );
        db.update(sql, HuskelappStatus.IKKE_AKTIV, huskelappId);
    }

    public Optional<String> hentNavkontorPaHuskelapp(Fnr brukerFnr){
        //Hvordan gjør vi dette ved støtte for flere huskelapper...
        String sql = String.format("SELECT ENHET_ID FROM %s WHERE %s=? AND STATUS = AKTIV", TABLE_NAME, FNR);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getString(ENHET_ID), brukerFnr.get())));
    }

    @SneakyThrows
    private static HuskelappOutputDto huskelappOutputListMapper(Map<String, Object> rs) {
        return new HuskelappOutputDto(
                (UUID) rs.get(HUSKELAPP_ID),
                Fnr.of((String) rs.get(FNR)),
                EnhetId.of((String) rs.get(ENHET_ID)),
                (Timestamp) rs.get(FRIST),
                (String) rs.get(KOMMENTAR));
    }
}
