package no.nav.fo.database;

import lombok.SneakyThrows;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.util.sql.SelectQuery;
import no.nav.fo.util.sql.SqlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class ArbeidslisteRepository {

    private static Logger LOG = LoggerFactory.getLogger(ArbeidslisteRepository.class);

    @Inject
    private JdbcTemplate jdbcTemplate;

    private static final String TABLE_NAME = "ARBEIDSLISTE";

    public Optional<Arbeidsliste> retrieveArbeidsliste(String aktoerId) {
        return new SelectQuery<Arbeidsliste>(jdbcTemplate, TABLE_NAME)
                .column("*")
                .whereEquals("AKTOERID", aktoerId)
                .usingMapper(this::arbeidslisteMapper)
                .execute();
    }

    public Optional<ArbeidslisteData> insertArbeidsliste(ArbeidslisteData data) {

        int inserted = SqlUtils.insert(jdbcTemplate, TABLE_NAME)
                .value("AKTOERID", data.getAktoerID())
                .value("VEILEDERIDENT", data.getVeilederId())
                .value("BESKRIVELSE", data.getKommentar())
                .value("FRIST", data.getFrist())
                .value("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                .execute();

        return inserted > 0 ? Optional.of(data) : Optional.empty();
    }

    public Optional<ArbeidslisteData> updateArbeidsliste(ArbeidslisteData data) {

        int updated = SqlUtils.update(jdbcTemplate, TABLE_NAME)
                .set("VEILEDERIDENT", data.getVeilederId())
                .set("BESKRIVELSE", data.getKommentar())
                .set("FRIST", data.getFrist())
                .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                .whereEquals("AKTOERID", data.getAktoerID())
                .execute();

        return updated > 0 ? Optional.of(data) : Optional.empty();
    }

    public Optional<String> deleteArbeidsliste(String aktoerID) {
        int updated = jdbcTemplate.update("DELETE FROM ARBEIDSLISTE WHERE AKTOERID = ?", aktoerID);
        return updated > 0 ? Optional.of(aktoerID) : Optional.empty();
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
