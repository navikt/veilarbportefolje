package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Repository
@RequiredArgsConstructor
public class FargekategoriRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public UUID upsertFargekateori(OppdaterFargekategoriRequest request, VeilederId sistEndretAv) {
        Timestamp sistEndret = toTimestamp(ZonedDateTime.now());

        String upsertSql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (fnr) DO UPDATE
                    SET verdi=?, sist_endret=?, sist_endret_av_veilederident=?
                """;

        jdbcTemplate.update(upsertSql, UUID.randomUUID(), request.fnr().get(), request.fargekategoriVerdi().name(), sistEndret, sistEndretAv.getValue(), request.fargekategoriVerdi().name(), sistEndret, sistEndretAv.getValue());

        return jdbcTemplate.queryForObject(
                "SELECT id FROM fargekategori WHERE fnr=?",
                (resultSet, rowNum) -> UUID.fromString(resultSet.getString("id")),
                request.fnr().get()
        );
    }
}
