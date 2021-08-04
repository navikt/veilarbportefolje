package no.nav.pto.veilarbportefolje.registrering;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_REGISTRERING.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

import java.time.format.DateTimeFormatter;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RegistreringRepositoryV2 {
    @NonNull
    @Qualifier("PostgresJdbc")
    private final JdbcTemplate db;

    public int upsertBrukerRegistrering(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp registreringOpprettetTimestamp = ofNullable(kafkaRegistreringMelding.getRegistreringOpprettet())
                .map(DateUtils::zonedDateStringToTimestamp)
                .orElse(null);

        return db.update("INSERT INTO " + TABLE_NAME +
                        " ("
                        + AKTOERID + ", "
                        + BRUKERS_SITUASJON + ", "
                        + REGISTRERING_OPPRETTET + ", "
                        + UTDANNING + ", "
                        + UTDANNING_BESTATT + ", "
                        + UTDANNING_GODKJENT + ") " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ") " +
                        "DO UPDATE SET ("
                        + BRUKERS_SITUASJON + ", "
                        + REGISTRERING_OPPRETTET + ", "
                        + UTDANNING + ", "
                        + UTDANNING_BESTATT + ", "
                        + UTDANNING_GODKJENT + ") = (?, ?, ?, ?, ?)",
                kafkaRegistreringMelding.getAktorid(),
                kafkaRegistreringMelding.getBrukersSituasjon(),
                registreringOpprettetTimestamp,
                kafkaRegistreringMelding.getUtdanning().toString(),
                kafkaRegistreringMelding.getUtdanningBestatt().toString(),
                kafkaRegistreringMelding.getUtdanningGodkjent().toString(),
                kafkaRegistreringMelding.getBrukersSituasjon(),
                registreringOpprettetTimestamp,
                kafkaRegistreringMelding.getUtdanning().toString(),
                kafkaRegistreringMelding.getUtdanningBestatt().toString(),
                kafkaRegistreringMelding.getUtdanningGodkjent().toString()
        );
    }

    public Optional<ArbeidssokerRegistrertEvent> hentBrukerRegistrering(AktorId aktoerId) {
        log.info("Hent BrukerRegistrering for bruker: {}", aktoerId.get());
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        return ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilArbeidssokerRegistrertEvent, aktoerId.get()))
        );
    }

    public int slettBrukerRegistrering(AktorId aktoerId) {
        log.info("Slett BrukerRegistrering for bruker: {}", aktoerId.get());
        return db.update(String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID), aktoerId.get());
    }

    @SneakyThrows
    private ArbeidssokerRegistrertEvent mapTilArbeidssokerRegistrertEvent(ResultSet rs, int i) {
        return ArbeidssokerRegistrertEvent.newBuilder()
                .setBrukersSituasjon(rs.getString("BRUKERS_SITUASJON"))
                .setAktorid(rs.getString(AKTOERID))
                .setUtdanning(UtdanningSvar.valueOf(rs.getString("UTDANNING")))
                .setUtdanningBestatt(UtdanningBestattSvar.valueOf(rs.getString("UTDANNING_BESTATT")))
                .setUtdanningGodkjent(UtdanningGodkjentSvar.valueOf(rs.getString("UTDANNING_GODKJENT")))
                .setRegistreringOpprettet(ofNullable(rs.getTimestamp("REGISTRERING_OPPRETTET"))
                        .map(DateUtils::toZonedDateTime)
                        .map(zonedDateRegistreringDato -> zonedDateRegistreringDato.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                        .orElse(null))
                .build();
    }
}
