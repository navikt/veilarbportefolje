package no.nav.pto.veilarbportefolje.registrering;

import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

public class RegistreringRepository {

    private JdbcTemplate db;
    private final String BRUKER_REGISTRERING_TABELL = "BRUKER_REGISTRERING";

    public RegistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertBrukerRegistrering(KafkaRegistreringMelding kafkaRegistreringMelding) {
        SqlUtils.upsert(db, BRUKER_REGISTRERING_TABELL)
                .set("AKTOERID", kafkaRegistreringMelding.getAktoerId().toString())
                .set("BRUKERSSITUASJON", kafkaRegistreringMelding.getBrukersSituasjon().toString())
                .where(WhereClause.equals("AKTOERID", kafkaRegistreringMelding.getAktoerId().toString()))
                .execute();
    }


    //TODO LYTTE PÅ AVSLUTTOPPFOLGINGFEEDEN OG SLETT BRUKEREN PGA DATAMINIMERING OSV MORRO
    public void slettBrukerRegistrering(KafkaRegistreringMelding kafkaRegistreringMelding) {
        SqlUtils.delete(db, BRUKER_REGISTRERING_TABELL)
                .where(WhereClause.equals("AKTOERID", kafkaRegistreringMelding.getAktoerId().toString()))
                .execute();
    }
}
