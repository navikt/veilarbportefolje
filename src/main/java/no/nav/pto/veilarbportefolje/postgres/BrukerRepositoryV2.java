package no.nav.pto.veilarbportefolje.postgres;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.util.DbUtils.mapTilOppfolgingsBruker;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;
import static no.nav.sbl.sql.SqlUtils.select;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BrukerRepositoryV2 {
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;
    private final UnleashService unleashService;

    public OppfolgingsBruker hentOppfolgingsBruker(AktorId aktorId) {
        return select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> mapTilOppfolgingsBruker(rs, unleashService))
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktorId.toString()))
                .execute();
    }


    public List<OppfolgingsBruker> hentOppfolgingsBrukere(List<AktorId> aktorIds) {
        db.setFetchSize(1000);
        List<String> ids = aktorIds.stream().map(AktorId::get).collect(toList());
        return SqlUtils
                .select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs, unleashService) : null)
                .column("*")
                .where(in("AKTOERID", ids))
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @SneakyThrows
    public static boolean erUnderOppfolging(ResultSet rs) {
        return parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING");
    }
}
