package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

public class ProfileringRepository {
    private JdbcTemplate db;
    private final String BRUKER_PROFILERING_TABELL = "BRUKER_PROFILERING";

    public ProfileringRepository(JdbcTemplate db) {
        this.db = db;
    }


    public void upsertBrukerProfilering (ArbeidssokerProfilertEvent kafkaMelding) {
      Timestamp timestamp = Timestamp.from(ZonedDateTime.parse(kafkaMelding.getProfileringGjennomfort()).toInstant());
        SqlUtils.upsert(db, BRUKER_PROFILERING_TABELL)
                .set("PROFILERING_RESULTAT", kafkaMelding.getProfilertTil().name())
                .set("AKTOERID", kafkaMelding.getAktorid())
                .set("PROFILERING_TIDSPUNKT", timestamp)
                .where(WhereClause.equals("AKTOERID", kafkaMelding.getAktorid()))
                .execute();
    }

    public ArbeidssokerProfilertEvent hentBrukerProfilering (AktoerId aktoerId) {
        return SqlUtils.select(db, BRUKER_PROFILERING_TABELL, this::mapTilArbeidssokerProfilertEvent)
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.aktoerId))
                .execute();
    }

    private ArbeidssokerProfilertEvent mapTilArbeidssokerProfilertEvent(ResultSet rs) throws SQLException {
        LocalDateTime localDateTime = rs.getTimestamp("PROFILERING_TIDSPUNKT").toLocalDateTime();
        return ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(rs.getString("AKTOERID"))
                .setProfileringGjennomfort(ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .setProfilertTil(ProfilertTil.valueOf(rs.getString("PROFILERING_RESULTAT")))
                .build();
    }
}
