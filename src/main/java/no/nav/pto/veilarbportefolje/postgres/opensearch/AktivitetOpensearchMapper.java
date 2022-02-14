package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetIkkeAktivStatuser;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetType;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetSamling;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.Arrays;
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
    private final static String aktivitetsplanenIkkeAktiveStatuser = Arrays.stream(AktivitetIkkeAktivStatuser.values())
            .map(Enum::name).collect(Collectors.joining(",", "{", "}"));

    public Map<String, AktivitetSamling> mapBulk(List<OppfolgingsBruker> brukere) {
        String aktoerIder = brukere.stream().map(OppfolgingsBruker::getAktoer_id).collect(Collectors.joining(",", "{", "}"));
        HashMap<String, AktivitetSamling> result = new HashMap<>(brukere.size());

        mapAvtalteAktiviteterFraAktivitetsplanen(aktoerIder, result);
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

    private void mapAvtalteAktiviteterFraAktivitetsplanen(String aktoerIder, HashMap<String, AktivitetSamling> results) {
        var params = new MapSqlParameterSource();
        params.addValue("ikkestatuser", aktivitetsplanenIkkeAktiveStatuser);
        params.addValue("ids", aktoerIder);
        db.query("""
                        SELECT aktoerid, tildato, fradato, aktivitettype FROM aktiviteter
                        WHERE avtalt AND NOT status = ANY (:ikkestatuser::varchar[]) AND aktoerid = ANY (:ids::varchar[])
                        """,
                params, (ResultSet rs) -> {
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
        String type = rs.getString("aktivitettype");
        if (!AktivitetType.contains(type)) {
            log.warn("Det finnes aktivteter i postgres som ikke blir vist i oversikten: {}", type);
        }
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setAktivitetType(AktivitetType.valueOf(type));
    }

    @SneakyThrows
    private AktivitetEntity mapGruppeAktivitetTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("moteplan_startdato"))
                .setUtlop(rs.getTimestamp("moteplan_sluttdato"))
                .setAktivitetType(AktivitetType.gruppeaktivitet);
    }

    @SneakyThrows
    private AktivitetEntity mapTiltakTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setAktivitetType(AktivitetType.tiltak);
    }

}
