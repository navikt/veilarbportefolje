package no.nav.pto.veilarbportefolje.profilering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.BRUKER_PROFILERING.*;
import static no.nav.pto.veilarbportefolje.database.PostgresTable.DIALOG.AKTOERID;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.pto.veilarbportefolje.postgres.PostgresUtils.queryForObjectOrNull;

import java.sql.ResultSet;
import java.util.Optional;

@Slf4j
@Repository
public class ProfileringRepositoryV2 {
    private final JdbcTemplate db;

    @Autowired
    public ProfileringRepositoryV2(@Qualifier("PostgresJdbc") JdbcTemplate db) {
        this.db = db;
    }

    public int upsertBrukerProfilering(ArbeidssokerProfilertEvent kafkaMelding) {
        log.info("Oppdaterer brukerprofilering i postgres for: {}, {}, {}", kafkaMelding.getAktorid(), kafkaMelding.getProfilertTil().name(), DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort()));
        return db.update("INSERT INTO " + TABLE_NAME +
                        " (" + AKTOERID + ", " + PROFILERING_RESULTAT + ", " + PROFILERING_TIDSPUNKT + ") " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (" + AKTOERID + ") " +
                        "DO UPDATE SET (" + PROFILERING_RESULTAT + ", " + PROFILERING_TIDSPUNKT + ") = (?, ?)",
                kafkaMelding.getAktorid(),
                kafkaMelding.getProfilertTil().name(),
                DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort()),
                kafkaMelding.getProfilertTil().name(),
                DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort())
        );
    }

    public Optional<ArbeidssokerProfilertEvent> hentBrukerProfilering(AktorId aktoerId) {
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", TABLE_NAME, AKTOERID);
        return Optional.ofNullable(
                queryForObjectOrNull(() -> db.queryForObject(sql, this::mapTilArbeidssokerProfilertEvent, aktoerId.get()))
        );
    }

    @SneakyThrows
    private ArbeidssokerProfilertEvent mapTilArbeidssokerProfilertEvent(ResultSet rs, int i) {
        return ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(rs.getString("AKTOERID"))
                .setProfileringGjennomfort(toZonedDateTime(rs.getTimestamp("PROFILERING_TIDSPUNKT")).format(ISO_ZONED_DATE_TIME))
                .setProfilertTil(ProfilertTil.valueOf(rs.getString("PROFILERING_RESULTAT")))
                .build();
    }

}
