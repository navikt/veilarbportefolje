package no.nav.pto.veilarbportefolje.fargekategori;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriController.OppdaterFargekategoriRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;

@Repository
@RequiredArgsConstructor
public class FargekategoriRepository {

    private final JdbcTemplate jdbcTemplate;

    public UUID oppdaterFargekateori(OppdaterFargekategoriRequest request, VeilederId sistEndretAv) {
        // TODO: Exception handling
        Timestamp sistEndret = toTimestamp(ZonedDateTime.now());

        String sql = """
                    INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (fnr) DO UPDATE
                    SET verdi=?, sist_endret=?, sist_endret_av_veilederident=?
                """;

        jdbcTemplate.update(sql, UUID.randomUUID(), request.fnr().get(), request.fargekategoriVerdi().verdi, sistEndret, sistEndretAv.getValue(), request.fargekategoriVerdi().verdi, sistEndret, sistEndretAv.getValue());

        return jdbcTemplate.queryForObject(
                "SELECT id FROM fargekategori WHERE fnr=?",
                (resultSet, rowNum) -> UUID.fromString(resultSet.getString("id")),
                request.fnr().get()
        );
    }
}
