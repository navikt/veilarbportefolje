package no.nav.fo.database;

import javaslang.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.util.sql.SelectQuery;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

import static no.nav.fo.util.sql.SqlUtils.update;
import static no.nav.fo.util.sql.SqlUtils.upsert;

public class ArbeidslisteRepository {

    @Inject
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "ARBEIDSLISTE";

    public Try<Arbeidsliste> retrieveArbeidsliste(String aktoerId) {
        return Try.of(
                () -> new SelectQuery<Arbeidsliste>(jdbcTemplate, TABLE_NAME)
                        .column("*")
                        .whereEquals("AKTOERID", aktoerId)
                        .usingMapper(this::arbeidslisteMapper)
                        .execute()
        );
    }

    public Try<Boolean> insertArbeidsliste(ArbeidslisteData data) {
        return Try.of(
                () -> upsert(jdbcTemplate, TABLE_NAME)
                        .set("AKTOERID", data.getAktoerID())
                        .set("VEILEDERIDENT", data.getVeilederId())
                        .set("BESKRIVELSE", data.getKommentar())
                        .set("FRIST", data.getFrist())
                        .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                        .where(WhereClause.equals("AKTOERID", data.getAktoerID()))
                        .execute()
        );
    }

    public Try<Integer> updateArbeidsliste(ArbeidslisteData data) {
        return Try.of(
                () -> update(jdbcTemplate, TABLE_NAME)
                        .set("VEILEDERIDENT", data.getVeilederId())
                        .set("BESKRIVELSE", data.getKommentar())
                        .set("FRIST", data.getFrist())
                        .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                        .whereEquals("AKTOERID", data.getAktoerID())
                        .execute()
        );
    }

    public Try<Integer> deleteArbeidsliste(String aktoerID) {
        int update = jdbcTemplate.update("DELETE FROM ARBEIDSLISTE WHERE AKTOERID = ?", aktoerID);

        if (update == 0) {
            return Try.failure(new RuntimeException("Kunne ikke slette rad fra database"));
        } else {
            return Try.success(1);
        }
    }

    @SneakyThrows
    private Arbeidsliste arbeidslisteMapper(ResultSet rs) {
        Arbeidsliste arbeidsliste = new Arbeidsliste();
        arbeidsliste.setVeilederId(rs.getString("VEILEDERIDENT"));
        arbeidsliste.setKommentar(rs.getString("BESKRIVELSE"));
        arbeidsliste.setFrist(rs.getTimestamp("FRIST"));
        arbeidsliste.setEndringstidspunkt(rs.getTimestamp("ENDRINGSTIDSPUNKT"));
        return arbeidsliste;
    }
}
