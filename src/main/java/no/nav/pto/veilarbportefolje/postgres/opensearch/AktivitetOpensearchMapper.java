package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetSamling;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AktivitetOpensearchMapper {
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate db;

    public Map<String, AktivitetSamling> mapBulk(List<OppfolgingsBruker> brukere) {
        String aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(Collectors.joining(",", "{", "}"));
        HashMap<String, AktivitetSamling> result = new HashMap<>(brukere.size());

        mapAktivitetsplanen(aktoerIder, result);
        mapGruppeAktiviteter(aktoerIder, result);
        mapTiltak(aktoerIder, result);

        return result;
    }

    private void mapTiltak(String aktoerIder, HashMap<String, AktivitetSamling> result) {
        db.query("""
                SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak
                WHERE aktoerid = ANY (:ids::varchar[])
                """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        String aktoerId = rs.getString("aktoerid");
                        AktivitetEntity aktivitet = mapTiltakTilEntity(rs);
                        updateHashTable(result, aktoerId, aktivitet, Optional.ofNullable(rs.getString("tiltakskode")));
                    }
                    return result;
                });
    }

    private void mapGruppeAktiviteter(String aktoerIder, HashMap<String, AktivitetSamling> result) {
        db.query("""
                        SELECT aktoerid, moteplan_startdato, moteplan_sluttdato FROM gruppe_aktiviter
                        WHERE date_trunc('day', moteplan_sluttdato) > date_trunc('day',current_timestamp)
                        AND aktiv = true AND aktoerid = ANY (:ids::varchar[])
                        """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        String aktoerId = rs.getString("aktoerid");
                        AktivitetEntity aktivitet = mapGruppeAktivitetTilEntity(rs);
                        updateHashTable(result, aktoerId, aktivitet, Optional.empty());
                    }
                    return result;
                });
    }

    private void mapAktivitetsplanen(String aktoerIder, HashMap<String, AktivitetSamling> results) {
        db.query("""
                SELECT aktoerid, tildato, fradato, aktivitettype FROM aktiviteter
                WHERE aktoerid = ANY (:ids::varchar[])
                """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        String aktoerId = rs.getString("aktoerid");
                        AktivitetEntity aktivitet = mapAktivitetTilEntity(rs);
                        updateHashTable(results, aktoerId, aktivitet, Optional.empty());
                    }
                    return results;
                });
    }

    private void updateHashTable(HashMap<String, AktivitetSamling> results, String aktoerId, AktivitetEntity aktivitet, Optional<String> tiltak) {
        Optional.ofNullable(results.get(aktoerId)).ifPresentOrElse(
                samling -> {
                    samling.getAvtalteAktiveAktivteter().add(aktivitet);
                    tiltak.ifPresent(t -> samling.getTiltak().add(t));
                },
                () -> {
                    AktivitetSamling samling = new AktivitetSamling();
                    samling.getAvtalteAktiveAktivteter().add(aktivitet);
                    tiltak.ifPresent(t -> samling.getTiltak().add(t));
                    results.put(aktoerId, samling);
                }
        );
    }


    @SneakyThrows
    private AktivitetEntity mapAktivitetTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setAktivitetType("aktivitettype");
    }

    @SneakyThrows
    private AktivitetEntity mapGruppeAktivitetTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("moteplan_startdato"))
                .setUtlop(rs.getTimestamp("moteplan_sluttdato"))
                .setAktivitetType("gruppeaktivitet");
    }

    @SneakyThrows
    private AktivitetEntity mapTiltakTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setAktivitetType("tiltak");
    }

}
