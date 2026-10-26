package no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.domene.InaktivAktivitetStatus;
import no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.leggTilAktivitetPaResultat;
import static no.nav.pto.veilarbportefolje.postgres.AktivitetEntityDto.mapTiltaksAktivitetTilEntity;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TiltakRepositoryV3 {

    private final JdbcTemplate db;
    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate namedDb;

    private static final String INAKTIV_AKTIVITET_STATUSER = Arrays.stream(InaktivAktivitetStatus.values())
            .map(Enum::name).collect(Collectors.joining(",", "{", "}"));

    public void leggTilTiltaksAktivitet(String aktorIder, boolean avtalt, HashMap<AktorId, List<AktivitetEntityDto>> result) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("aktorIder", aktorIder);
        params.addValue("avtalt", avtalt);
        params.addValue("aktivitetstype", "TILTAK");
        params.addValue("ikkestatuser", INAKTIV_AKTIVITET_STATUSER);

        String sql = """
        SELECT AKTOR_ID,
               CAST(TIL_DATO AS TIMESTAMP) AS TIL_DATO,
               CAST(FRA_DATO AS TIMESTAMP) AS FRA_DATO,
               TILTAKSKODE FROM KAFKA_AKTIVITET_MELDING
        WHERE AKTOR_ID = ANY (:aktorIder::varchar[])
        AND AKTIVITET_TYPE = :aktivitetstype::varchar
        AND AVTALT = :avtalt::boolean
        AND NOT (AKTIVITET_STATUS = ANY (:ikkestatuser::varchar[]))
        """;

        namedDb.query(sql,
                params,
                (ResultSet rs) -> {
                    while (rs.next()) {
                        AktorId aktoerId = AktorId.of(rs.getString("AKTOR_ID"));
                        AktivitetEntityDto aktivitet = mapTiltaksAktivitetTilEntity(rs);

                        result.compute(aktoerId, (k, list) -> leggTilAktivitetPaResultat(aktivitet, list));
                    }
                    return result;
                });
    }
}
