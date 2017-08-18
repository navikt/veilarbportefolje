package no.nav.fo.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.domene.EnhetTiltak;
import no.nav.fo.util.sql.where.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static no.nav.fo.util.sql.SqlUtils.*;

public class EnhetTiltakRepository {
    private static Logger LOG = LoggerFactory.getLogger(EnhetTiltakRepository.class);

    @Inject
    private JdbcTemplate db;

    @Inject
    private DataSource ds;

    public static final String ENHETTILTAK = "ENHETTILTAK";

    public Try<EnhetTiltak> retrieveEnhettiltak(String enhet) {
        return Try.of(
                () -> select(ds, ENHETTILTAK, EnhetTiltakRepository::enhettiltakMapper)
                        .column("TILTAKSKODE")
                        .where(WhereClause.equals("ENHETID", enhet))
                        .execute()
                        .setEnhet(enhet)
        );
    }

    @SneakyThrows
    private static EnhetTiltak enhettiltakMapper(ResultSet rs) {
        List<String> tiltak = new ArrayList<>();

        while(rs.next()) {
            tiltak.add(rs.getString("tiltakskode"));
        }

        return new EnhetTiltak()
                .setTiltak(tiltak);
    }

}
