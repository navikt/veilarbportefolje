package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Repository
@RequiredArgsConstructor
public class FargekategoriRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<FargekategoriEntity> hentFargekategoriForBruker(Fnr fnr) {
        String hentSql = "SELECT * FROM fargekategori WHERE fnr=?";

        return Optional.ofNullable(queryForObjectOrNull(() ->
                jdbcTemplate.queryForObject(
                        hentSql,
                        (resultSet, rowNumber) -> FargekategoriMapper.fargekategoriMapper(resultSet),
                        fnr.get())
        ));
    }

    @Transactional
    public FargekategoriEntity upsertFargekateori(OppdaterFargekategoriRequest request, VeilederId sistEndretAv) {
        Timestamp sistEndret = toTimestamp(ZonedDateTime.now());

        String upsertSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (fnr) DO UPDATE
                    SET verdi=excluded.verdi, sist_endret=excluded.sist_endret, sist_endret_av_veilederident=excluded.sist_endret_av_veilederident, enhet_id=excluded.enhet_id
                """;

        jdbcTemplate.update(upsertSql,
                UUID.randomUUID(),
                request.fnr().get(),
                request.fargekategoriVerdi().name(),
                sistEndret,
                sistEndretAv.getValue(),
                request.enhetId().get());

        return jdbcTemplate.queryForObject(
                "SELECT * FROM fargekategori WHERE fnr=?",
                (resultSet, rowNum) -> FargekategoriMapper.fargekategoriMapper(resultSet),
                request.fnr().get()
        );
    }

    public void deleteFargekategori(Fnr fnr) {
        String deleteSql = "DELETE FROM fargekategori WHERE fnr=?";
        jdbcTemplate.update(deleteSql, fnr.get());
    }


    public void batchdeleteFargekategori(List<Fnr> fnr) {
        String deleteSql = "DELETE FROM fargekategori WHERE fnr=?";

        jdbcTemplate.batchUpdate(deleteSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, fnr.get(i).get());
            }

            @Override
            public int getBatchSize() {
                return fnr.size();
            }
        });
    }

    public void batchupsertFargekategori(FargekategoriVerdi fargekategoriVerdi, List<Fnr> fnr, VeilederId sisteEndretAv, EnhetId enhetId) {
        String upsertSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident, enhet_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (fnr) DO UPDATE
                    SET verdi=excluded.verdi, sist_endret=excluded.sist_endret, sist_endret_av_veilederident=excluded.sist_endret_av_veilederident
                """;

        jdbcTemplate.batchUpdate(upsertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setObject(1, UUID.randomUUID().toString(), java.sql.Types.OTHER);
                ps.setString(2, fnr.get(i).get());
                ps.setString(3, fargekategoriVerdi.name());
                ps.setTimestamp(4, toTimestamp(ZonedDateTime.now()));
                ps.setString(5, sisteEndretAv.getValue());
                ps.setString(6, enhetId.get());
            }

            @Override
            public int getBatchSize() {
                return fnr.size();
            }
        });
    }

    public Optional<String> hentNavkontorPaFargekategori(Fnr fnr) {
        String hentSql = "SELECT enhet_id FROM fargekategori WHERE fnr=?";

        return Optional.ofNullable(queryForObjectOrNull(() ->
                jdbcTemplate.queryForObject(
                        hentSql,
                        (resultSet, rowNumber) -> resultSet.getString("enhet_id"),
                        fnr.get())
        ));
    }
}
