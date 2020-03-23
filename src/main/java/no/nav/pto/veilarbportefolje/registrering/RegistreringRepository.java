package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistreringRepository {

    private JdbcTemplate db;
    private final String BRUKER_REGISTRERING_TABELL = "BRUKER_REGISTRERING";

    public RegistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertBrukerRegistrering(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        SqlUtils.upsert(db, BRUKER_REGISTRERING_TABELL)
                .set("AKTOERID", kafkaRegistreringMelding.getAktorid())
                .set("BRUKERS_SITUASJON", kafkaRegistreringMelding.getBrukersSituasjon())
                .where(WhereClause.equals("AKTOERID", kafkaRegistreringMelding.getAktorid()))
                .execute();
    }

    public ArbeidssokerRegistrertEvent hentBrukerRegistrering(AktoerId aktoerId) {
        return SqlUtils.select(db, BRUKER_REGISTRERING_TABELL, RegistreringRepository::mapTilArbeidssokerRegistrertEvent)
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();
    }

    //TODO LYTTE PÅ AVSLUTTOPPFOLGINGFEEDEN OG SLETT BRUKEREN PGA DATAMINIMERING OSV MORRO
    public void slettBrukerRegistrering(AktoerId aktoerId) {
        SqlUtils.delete(db, BRUKER_REGISTRERING_TABELL)
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();
    }

    private static ArbeidssokerRegistrertEvent mapTilArbeidssokerRegistrertEvent (ResultSet rs) throws SQLException {
        return ArbeidssokerRegistrertEvent.newBuilder()
                .setBrukersSituasjon(rs.getString("BRUKERS_SITUASJON"))
                .setAktorid(rs.getString("AKTOERID"))
                .setRegistreringOpprettet(null)
                .build();
    }
}
