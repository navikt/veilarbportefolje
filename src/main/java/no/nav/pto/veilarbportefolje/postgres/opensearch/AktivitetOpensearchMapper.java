package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetIkkeAktivStatuser;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetsType;
import no.nav.pto.veilarbportefolje.postgres.opensearch.utils.AktivitetEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.util.ArrayList;
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

    public Map<AktorId, List<AktivitetEntity>> mapBulk(List<AktorId> brukere) {
        String aktoerIder = brukere.stream().map(AktorId::get).collect(Collectors.joining(",", "{", "}"));
        HashMap<AktorId, List<AktivitetEntity>> result = new HashMap<>(brukere.size());

        mapAvtalteAktiviteterFraAktivitetsplanen(aktoerIder, result);
        mapGruppeAktiviteter(aktoerIder, result);
        mapTiltak(aktoerIder, result);

        return result;
    }

    private void mapTiltak(String aktoerIder, HashMap<AktorId, List<AktivitetEntity>> result) {
        db.query("""
                        SELECT aktoerid, tildato, fradato, tiltakskode FROM brukertiltak
                        WHERE aktoerid = ANY (:ids::varchar[])
                        """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("aktoerid"));
                        AktivitetEntity aktivitet = mapTiltakTilEntity(rs);

                        Optional.ofNullable(result.get(aktoerId)).ifPresentOrElse(
                                samling -> samling.add(aktivitet),
                                () -> {
                                    ArrayList<AktivitetEntity> liste = new ArrayList<>();
                                    liste.add(aktivitet);
                                    result.put(aktoerId, liste);
                                }
                        );
                    }
                    return result;
                });
    }

    private void mapGruppeAktiviteter(String aktoerIder, HashMap<AktorId, List<AktivitetEntity>> result) {
        db.query("""
                        SELECT aktoerid, moteplan_startdato, moteplan_sluttdato FROM gruppe_aktiviter
                        WHERE date_trunc('day', moteplan_sluttdato) > date_trunc('day',current_timestamp)
                        AND aktiv = true AND aktoerid = ANY (:ids::varchar[])
                        """,
                new MapSqlParameterSource("ids", aktoerIder),
                (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("aktoerid"));
                        AktivitetEntity aktivitet = mapGruppeAktivitetTilEntity(rs);

                        Optional.ofNullable(result.get(aktoerId)).ifPresentOrElse(
                                samling -> samling.add(aktivitet),
                                () -> {
                                    ArrayList<AktivitetEntity> liste = new ArrayList<>();
                                    liste.add(aktivitet);
                                    result.put(aktoerId, liste);
                                }
                        );
                    }
                    return result;
                });
    }

    private void mapAvtalteAktiviteterFraAktivitetsplanen(String aktoerIder, HashMap<AktorId, List<AktivitetEntity>> result) {
        var params = new MapSqlParameterSource();
        params.addValue("ikkestatuser", aktivitetsplanenIkkeAktiveStatuser);
        params.addValue("ids", aktoerIder);
        db.query("""
                        SELECT aktoerid, tildato, fradato, aktivitettype FROM aktiviteter
                        WHERE avtalt AND NOT status = ANY (:ikkestatuser::varchar[]) AND aktoerid = ANY (:ids::varchar[])
                        """,
                params, (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("aktoerid"));
                        AktivitetEntity aktivitet = mapAktivitetTilEntity(rs);

                        Optional.ofNullable(result.get(aktoerId)).ifPresentOrElse(
                                samling -> samling.add(aktivitet),
                                () -> {
                                    ArrayList<AktivitetEntity> liste = new ArrayList<>();
                                    liste.add(aktivitet);
                                    result.put(aktoerId, liste);
                                }
                        );
                    }
                    return result;
                });
    }


    @SneakyThrows
    private AktivitetEntity mapAktivitetTilEntity(ResultSet rs) {
        String type = rs.getString("aktivitettype");
        if (!AktivitetsType.contains(type)) {
            log.warn("Det finnes aktivteter i postgres som ikke blir vist i oversikten: {}", type);
        }
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setAktivitetsType(AktivitetsType.valueOf(type));
    }

    @SneakyThrows
    private AktivitetEntity mapGruppeAktivitetTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("moteplan_startdato"))
                .setUtlop(rs.getTimestamp("moteplan_sluttdato"))
                .setAktivitetsType(AktivitetsType.gruppeaktivitet);
    }

    @SneakyThrows
    private AktivitetEntity mapTiltakTilEntity(ResultSet rs) {
        return new AktivitetEntity()
                .setStart(rs.getTimestamp("fradato"))
                .setUtlop(rs.getTimestamp("tildato"))
                .setMuligTiltaksNavn(rs.getString("tiltakskode"))
                .setAktivitetsType(AktivitetsType.tiltak);
    }

}
