package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.util.DbUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RegistreringRepository {

    private JdbcTemplate db;
    private final String BRUKER_REGISTRERING_TABELL = "BRUKER_REGISTRERING";

    public RegistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertBrukerRegistrering(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp timestamp = Timestamp.from((ZonedDateTime.parse(kafkaRegistreringMelding.getRegistreringOpprettet()).toInstant()));
        SqlUtils.insert(db, BRUKER_REGISTRERING_TABELL)
                .value("AKTOERID", kafkaRegistreringMelding.getAktorid())
                .value("BRUKERS_SITUASJON", kafkaRegistreringMelding.getBrukersSituasjon())
                .value("REGISTRERING_OPPRETTET", timestamp)
                .value("SIST_OPPDATERT", new Timestamp(System.currentTimeMillis()))
                .execute();
    }

    public void uppdaterBrukerRegistring(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp timestamp = Timestamp.from((ZonedDateTime.parse(kafkaRegistreringMelding.getRegistreringOpprettet()).toInstant()));
        SqlUtils.update(db, BRUKER_REGISTRERING_TABELL)
                .set("BRUKERS_SITUASJON", kafkaRegistreringMelding.getBrukersSituasjon())
                .set("REGISTRERING_OPPRETTET", timestamp)
                .set("SIST_OPPDATERT", new Timestamp(System.currentTimeMillis()))
                .whereEquals("AKTOERID", kafkaRegistreringMelding.getAktorid())
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
        ZonedDateTime zonedDateTime = ZonedDateTime.of(rs.getTimestamp("REGISTRERING_OPPRETTET").toLocalDateTime(), ZoneId.systemDefault());
        return ArbeidssokerRegistrertEvent.newBuilder()
                .setBrukersSituasjon(rs.getString("BRUKERS_SITUASJON"))
                .setAktorid(rs.getString("AKTOERID"))
                .setRegistreringOpprettet(zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .build();
    }
}
