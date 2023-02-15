package no.nav.pto.veilarbportefolje.registrering;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_REGISTRERING.*;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RegistreringRepositoryV2 {
    private final JdbcTemplate db;

    public void upsertBrukerRegistrering(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        Timestamp registreringOpprettetTimestamp = ofNullable(kafkaRegistreringMelding.getRegistreringOpprettet())
                .map(DateUtils::zonedDateStringToTimestamp)
                .orElse(null);

        db.update("""
                        INSERT INTO BRUKER_REGISTRERING
                        (AKTOERID, BRUKERS_SITUASJON, REGISTRERING_OPPRETTET,
                        UTDANNING, UTDANNING_BESTATT, UTDANNING_GODKJENT)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (AKTOERID)
                        DO UPDATE SET
                        (BRUKERS_SITUASJON, REGISTRERING_OPPRETTET,
                        UTDANNING, UTDANNING_BESTATT, UTDANNING_GODKJENT ) =
                        (excluded.brukers_situasjon, excluded.registrering_opprettet, excluded.utdanning, excluded.utdanning_bestatt, excluded.utdanning_godkjent)
                        """,
                kafkaRegistreringMelding.getAktorid(),
                kafkaRegistreringMelding.getBrukersSituasjon(),
                registreringOpprettetTimestamp,
                Optional.ofNullable(kafkaRegistreringMelding.getUtdanning()).map(UtdanningSvar::toString).orElse(null),
                Optional.ofNullable(kafkaRegistreringMelding.getUtdanningBestatt()).map(UtdanningBestattSvar::toString).orElse(null),
                Optional.ofNullable(kafkaRegistreringMelding.getUtdanningGodkjent()).map(UtdanningGodkjentSvar::toString).orElse(null)
        );
    }

    public Optional<ArbeidssokerRegistrertEvent> hentBrukerRegistrering(AktorId aktoerId) {
        secureLog.info("Hent BrukerRegistrering for bruker: {}", aktoerId.get());
        String sql = "SELECT * FROM BRUKER_REGISTRERING WHERE AKTOERID = ?";
        return ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilArbeidssokerRegistrertEvent, aktoerId.get()))
        );
    }

    public int slettBrukerRegistrering(AktorId aktoerId) {
        return db.update("DELETE FROM BRUKER_REGISTRERING WHERE AKTOERID = ?", aktoerId.get());
    }

    @SneakyThrows
    private ArbeidssokerRegistrertEvent mapTilArbeidssokerRegistrertEvent(ResultSet rs, int i) {
        return ArbeidssokerRegistrertEvent.newBuilder()
                .setBrukersSituasjon(rs.getString(BRUKERS_SITUASJON))
                .setAktorid(rs.getString(AKTOERID))
                .setUtdanning(Optional.ofNullable(rs.getString(UTDANNING)).map(UtdanningSvar::valueOf).orElse(null))
                .setUtdanningBestatt(Optional.ofNullable(rs.getString(UTDANNING_BESTATT)).map(UtdanningBestattSvar::valueOf).orElse(null))
                .setUtdanningGodkjent(Optional.ofNullable(rs.getString(UTDANNING_GODKJENT)).map(UtdanningGodkjentSvar::valueOf).orElse(null))
                .setRegistreringOpprettet(ofNullable(rs.getTimestamp(REGISTRERING_OPPRETTET))
                        .map(DateUtils::toZonedDateTime)
                        .map(zonedDateRegistreringDato -> zonedDateRegistreringDato.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                        .orElse(null))
                .build();
    }

    public List<AktorId> hentAlleBrukereMedRegistrering() {
        return db.queryForList("SELECT DISTINCT aktoerid FROM bruker_registrering", AktorId.class);
    }
}
