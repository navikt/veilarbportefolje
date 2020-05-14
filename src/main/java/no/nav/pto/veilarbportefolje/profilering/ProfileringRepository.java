package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

public class ProfileringRepository {
    private JdbcTemplate db;
    private final String BRUKER_PROFILERING_TABELL = "BRUKER_PROFILERING";

    public ProfileringRepository(JdbcTemplate db) {
        this.db = db;
    }


    public void upsertBrukerProfilering (ArbeidssokerProfilertEvent kafkaMelding) {
        SqlUtils.upsert(db, BRUKER_PROFILERING_TABELL)
                .set("PROFILERING_RESULTAT", kafkaMelding.getProfilertTil().name())
                .set("PROFILERING_TIDSPUNKT", Timestamp.valueOf(kafkaMelding.getProfileringGjennomfort()))
                .where(WhereClause.equals("AKTOERID", kafkaMelding.getAktorid()))
                .execute();
    }
}
