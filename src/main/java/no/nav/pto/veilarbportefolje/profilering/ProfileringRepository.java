package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime;

@Repository
public class ProfileringRepository {
    private JdbcTemplate db;
    public final String BRUKER_PROFILERING_TABELL = "BRUKER_PROFILERING";

    @Autowired
    public ProfileringRepository(JdbcTemplate db) {
        this.db = db;
    }

    //TODO SLETT PROFILERINGSRESULTAT NÅR EN BRUKARE FÅR VEDTAK 14a???

    public void upsertBrukerProfilering(ArbeidssokerProfilertEvent kafkaMelding) {
        Timestamp timestamp = DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort());
        SqlUtils.upsert(db, BRUKER_PROFILERING_TABELL)
                .set("PROFILERING_RESULTAT", kafkaMelding.getProfilertTil().name())
                .set("AKTOERID", kafkaMelding.getAktorid())
                .set("PROFILERING_TIDSPUNKT", timestamp)
                .where(WhereClause.equals("AKTOERID", kafkaMelding.getAktorid()))
                .execute();
    }

    public ArbeidssokerProfilertEvent hentBrukerProfilering (AktorId aktoerId) {
        return SqlUtils.select(db, BRUKER_PROFILERING_TABELL, this::mapTilArbeidssokerProfilertEvent)
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();
    }

    private ArbeidssokerProfilertEvent mapTilArbeidssokerProfilertEvent(ResultSet rs) throws SQLException {
        return ArbeidssokerProfilertEvent.newBuilder()
                .setAktorid(rs.getString("AKTOERID"))
                .setProfileringGjennomfort(toZonedDateTime(rs.getTimestamp("PROFILERING_TIDSPUNKT")).format(ISO_ZONED_DATE_TIME))
                .setProfilertTil(ProfilertTil.valueOf(rs.getString("PROFILERING_RESULTAT")))
                .build();
    }
}
