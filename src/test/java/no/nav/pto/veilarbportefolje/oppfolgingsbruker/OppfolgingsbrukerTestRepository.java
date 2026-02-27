package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.common.types.identer.Fnr;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.database.PostgresTable.OPPFOLGINGSBRUKER_ARENA_V2.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Repository
@RequiredArgsConstructor
public class OppfolgingsbrukerTestRepository {
    @Qualifier("PostgresNamedJdbcReadOnly")
    private final NamedParameterJdbcTemplate dbNamed;

    public Optional<OppfolgingsbrukerEntity> getOppfolgingsBruker(Fnr fnr) {
        String sql = """
            SELECT
                OPPFOLGINGSBRUKER_ARENA_V2.FODSELSNR,
                OPPFOLGINGSBRUKER_ARENA_V2.FORMIDLINGSGRUPPEKODE,
                OPPFOLGINGSBRUKER_ARENA_V2.ISERV_FRA_DATO,
                coalesce(ao_kontor.kontor_id, OPPFOLGINGSBRUKER_ARENA_V2.NAV_KONTOR) as NAV_KONTOR,
                OPPFOLGINGSBRUKER_ARENA_V2.KVALIFISERINGSGRUPPEKODE,
                OPPFOLGINGSBRUKER_ARENA_V2.RETTIGHETSGRUPPEKODE,
                OPPFOLGINGSBRUKER_ARENA_V2.HOVEDMAALKODE,
                OPPFOLGINGSBRUKER_ARENA_V2.ENDRET_DATO
            FROM BRUKER_IDENTER initiellIdent
            INNER JOIN BRUKER_IDENTER alleIdenter on alleIdenter.person = initiellIdent.person
            JOIN OPPFOLGINGSBRUKER_ARENA_V2 on alleIdenter.ident = OPPFOLGINGSBRUKER_ARENA_V2.fodselsnr
            LEFT JOIN ao_kontor on ao_kontor.ident = OPPFOLGINGSBRUKER_ARENA_V2.fodselsnr
            WHERE initiellIdent.ident = :ident
        """;
        return Optional.ofNullable(
                queryForObjectOrNull(() -> dbNamed.queryForObject(
                        sql,
                        new MapSqlParameterSource().addValue("ident", fnr.get()),
                        OppfolgingsbrukerTestRepository::mapTilOppfolgingsbruker
                ))
        );
    }

    @SneakyThrows
    private static OppfolgingsbrukerEntity mapTilOppfolgingsbruker(ResultSet rs, int row) {
        if (rs == null || rs.getString(FODSELSNR) == null) {
            return null;
        }
        return new OppfolgingsbrukerEntity(
                rs.getString(FODSELSNR),
                rs.getString(FORMIDLINGSGRUPPEKODE),
                toZonedDateTime(rs.getTimestamp(ISERV_FRA_DATO)),
                rs.getString(NAV_KONTOR),
                rs.getString(KVALIFISERINGSGRUPPEKODE),
                rs.getString(RETTIGHETSGRUPPEKODE),
                rs.getString(HOVEDMAALKODE),
                toZonedDateTime(rs.getTimestamp(ENDRET_DATO)));
    }
}
