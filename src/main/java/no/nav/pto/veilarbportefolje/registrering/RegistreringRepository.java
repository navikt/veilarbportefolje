package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.*;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_REGISTRERING.AKTOERID;

@Repository
public class RegistreringRepository {

    private final JdbcTemplate db;

    @Autowired
    public RegistreringRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void upsertBrukerRegistrering(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp timestamp = ofNullable(kafkaRegistreringMelding.getRegistreringOpprettet())
                .map(DateUtils::zonedDateStringToTimestamp)
                .orElse(null);

        SqlUtils.upsert(db, Table.BRUKER_REGISTRERING.TABLE_NAME)
                .set(AKTOERID, kafkaRegistreringMelding.getAktorid())
                .set("BRUKERS_SITUASJON", kafkaRegistreringMelding.getBrukersSituasjon())
                .set("KAFKA_MELDING_MOTTATT", new Timestamp(System.currentTimeMillis()))
                .set("REGISTRERING_OPPRETTET", timestamp)
                .set("UTDANNING", kafkaRegistreringMelding.getUtdanning().toString())
                .set("UTDANNING_BESTATT", kafkaRegistreringMelding.getUtdanningBestatt().toString())
                .set("UTDANNING_GODKJENT", kafkaRegistreringMelding.getUtdanningGodkjent().toString())
                .where(WhereClause.equals(AKTOERID, kafkaRegistreringMelding.getAktorid()))
                .execute();
    }


    public Optional<ArbeidssokerRegistrertEvent> hentBrukerRegistrering(AktorId aktoerId) {
        return ofNullable(
                SqlUtils.select(db, Table.BRUKER_REGISTRERING.TABLE_NAME, RegistreringRepository::mapTilArbeidssokerRegistrertEvent)
                        .column("*")
                        .where(WhereClause.equals(AKTOERID, aktoerId.get()))
                        .execute()
        );
    }

    public void slettBrukerRegistrering(AktorId aktoerId) {
        SqlUtils.delete(db, Table.BRUKER_REGISTRERING.TABLE_NAME)
                .where(WhereClause.equals(AKTOERID, aktoerId.get()))
                .execute();
    }

    private static ArbeidssokerRegistrertEvent mapTilArbeidssokerRegistrertEvent(ResultSet rs) throws SQLException {
        String registreringOpprettet = ofNullable(rs.getTimestamp("REGISTRERING_OPPRETTET"))
                .map(DateUtils::toZonedDateTime)
                .map(zonedDateRegistreringDato -> zonedDateRegistreringDato.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .orElse(null);

        return ArbeidssokerRegistrertEvent.newBuilder()
                .setBrukersSituasjon(rs.getString("BRUKERS_SITUASJON"))
                .setAktorid(rs.getString(AKTOERID))
                .setUtdanning(UtdanningSvar.valueOf(rs.getString("UTDANNING")))
                .setUtdanningBestatt(UtdanningBestattSvar.valueOf(rs.getString("UTDANNING_BESTATT")))
                .setUtdanningGodkjent(UtdanningGodkjentSvar.valueOf(rs.getString("UTDANNING_GODKJENT")))
                .setRegistreringOpprettet(registreringOpprettet)
                .build();
    }
}
