package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.GruppeAktivitetInnhold;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.arenapakafka.ArenaUtils.getLocalDateTimeOrNull;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.GRUPPE_AKTIVITER.*;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.leggTilAktivitetPaResultat;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.mapGruppeAktivitetTilEntity;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;


@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class GruppeAktivitetRepositoryV2 {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    public void upsertGruppeAktivitet(GruppeAktivitetInnhold aktivitet, AktorId aktorId, boolean aktiv) {
        // Fra dato kan ha verdien null, det tilsier at aktiviteten varer en hel dag
        LocalDateTime tilDato = getLocalDateTimeOrNull(aktivitet.getAktivitetperiodeTil(), true);
        LocalDateTime fraDato = Optional.ofNullable(getLocalDateTimeOrNull(aktivitet.getAktivitetperiodeFra(), false))
                .orElse(LocalDateTime.of(tilDato.toLocalDate(), LocalTime.MIDNIGHT));

        db.update("""
                        INSERT INTO gruppe_aktiviter
                        (MOTEPLAN_ID, VEILEDNINGDELTAKER_ID, AKTOERID, MOTEPLAN_STARTDATO, MOTEPLAN_SLUTTDATO, HENDELSE_ID, AKTIV)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (MOTEPLAN_ID, VEILEDNINGDELTAKER_ID)
                        DO UPDATE SET (AKTOERID, MOTEPLAN_STARTDATO, MOTEPLAN_SLUTTDATO, HENDELSE_ID, AKTIV)
                        = (excluded.aktoerid, excluded.moteplan_startdato, excluded.moteplan_sluttdato, excluded.hendelse_id, excluded.aktiv)
                        """,
                aktivitet.getMoteplanId(), aktivitet.getVeiledningdeltakerId(),
                aktorId.get(), fraDato, tilDato, aktivitet.getHendelseId(), aktiv
        );
    }

    public Optional<Long> retrieveHendelse(GruppeAktivitetInnhold aktivitet) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ? AND  %s = ?", TABLE_NAME, MOTEPLAN_ID, VEILEDNINGDELTAKER_ID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, (rs, row) -> rs.getLong(HENDELSE_ID), aktivitet.getMoteplanId(), aktivitet.getVeiledningdeltakerId()))
        );
    }

    public void leggTilGruppeAktiviteter(String aktoerIder, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        namedDb.query("""
                        SELECT aktoerid, moteplan_startdato, moteplan_sluttdato FROM gruppe_aktiviter
                        WHERE date_trunc('day', moteplan_sluttdato) >= date_trunc('day',current_timestamp)
                        AND aktiv = true AND aktoerid = ANY (:ids::varchar[])
                        """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("aktoerid"));
                        AktivitetEntityDto aktivitet = mapGruppeAktivitetTilEntity(rs);

                        List<AktivitetEntityDto> list = result.get(aktoerId);
                        result.put(aktoerId, leggTilAktivitetPaResultat(aktivitet, list));
                    }
                    return result;
                });
    }
}
