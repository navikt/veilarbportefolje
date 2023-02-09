package no.nav.pto.veilarbportefolje.ensligforsorger;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerTiltak;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull;

@Slf4j
@Repository
@RequiredArgsConstructor
public class EnsligeForsorgereRepository {
    private final JdbcTemplate db;

    @Qualifier("PostgresJdbcReadOnly")
    private final JdbcTemplate dbReadOnly;

    public void lagreDataForEnsligeForsorgere() {

    }

    public Optional<EnsligeForsorgerTiltak> hentDataForEnsligeForsorgere(String personIdent) {
        String sql = """
                 SELECT efp.fra_dato,
                       efp.til_dato,
                       vperiode_type.PERIODE_TYPE,
                       EAT.AKTIVITET_TYPE
                FROM enslige_forsorgere ef,
                     enslige_forsorgere_periode efp
                LEFT JOIN EF_VEDTAKSPERIODE_TYPE vperiode_type on efp.PERIODETYPE = vperiode_type.ID
                LEFT JOIN EF_AKTIVITET_TYPE EAT on efp.AKTIVITETSTYPE = EAT.ID
                LEFT JOIN EF_VEDTAKSRESULTAT_TYPE EVT on ef.VEDTAKSRESULTAT = EVT.ID
                LEFT JOIN EF_STONAD_TYPE EST on EST.ID = ef.STØNADSTYPE
                WHERE ef.vedtakId = efp.vedtakId
                  AND est.STONAD_TYPE = 'OVERGANGSSTØNAD'
                  AND EVT.VEDTAKSRESULTAT_TYPE = 'INNVILGET'
                  AND ef.personIdent = ?;
                 """;
        return dbReadOnly.queryForList(sql, personIdent)
                .stream().map(this::mapTilTiltak)
                .max(Comparator.comparing(EnsligeForsorgerTiltak::til_dato));
    }

    @SneakyThrows
    private EnsligeForsorgerTiltak mapTilTiltak(Map<String, Object> rs) {
        return new EnsligeForsorgerTiltak(toLocalDateOrNull(String.valueOf(rs.get("fra_dato"))), toLocalDateOrNull(String.valueOf(rs.get("til_dato"))));
    }


}
