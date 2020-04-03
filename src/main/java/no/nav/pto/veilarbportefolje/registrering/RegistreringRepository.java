package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class RegistreringRepository {

    private JdbcTemplate db;
    private final String BRUKER_REGISTRERING_TABELL = "BRUKER_REGISTRERING";

    public RegistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertBrukerRegistrering(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp timestamp = Optional.ofNullable(kafkaRegistreringMelding.getRegistreringOpprettet())
                .map(registreringOpprettet ->Timestamp.from((ZonedDateTime.parse(registreringOpprettet).toInstant())))
                .orElse(null);

        SqlUtils.upsert(db, BRUKER_REGISTRERING_TABELL)
                .set("AKTOERID", kafkaRegistreringMelding.getAktorid())
                .set("BRUKERS_SITUASJON", kafkaRegistreringMelding.getBrukersSituasjon())
                .set("KAFKA_MELDING_MOTTATT", new Timestamp(System.currentTimeMillis()))
                .set("REGISTRERING_OPPRETTET", timestamp)
                .where(WhereClause.equals("AKTOERID", kafkaRegistreringMelding.getAktorid()))
                .execute();
    }

    public void oppdaterBrukerRegistring(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp timestamp = Timestamp.from((ZonedDateTime.parse(kafkaRegistreringMelding.getRegistreringOpprettet()).toInstant()));
        SqlUtils.update(db, BRUKER_REGISTRERING_TABELL)
                .set("BRUKERS_SITUASJON", kafkaRegistreringMelding.getBrukersSituasjon())
                .set("REGISTRERING_OPPRETTET", timestamp)
                .set("KAFKA_MELDING_MOTTATT", new Timestamp(System.currentTimeMillis()))
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
        String registreringOpprettet = Optional.ofNullable(rs.getTimestamp("REGISTRERING_OPPRETTET"))
                .map(registreringDato -> ZonedDateTime.of(registreringDato.toLocalDateTime(), ZoneId.systemDefault()))
                .map(zonedDateRegistreringDato -> zonedDateRegistreringDato.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .orElse(null);

        return ArbeidssokerRegistrertEvent.newBuilder()
                .setBrukersSituasjon(rs.getString("BRUKERS_SITUASJON"))
                .setAktorid(rs.getString("AKTOERID"))
                .setRegistreringOpprettet(registreringOpprettet)
                .build();
    }
}
