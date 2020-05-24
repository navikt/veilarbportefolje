package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.profilering.ProfilertTil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

public class ProfileringRepository {
    private JdbcTemplate db;
    public final String BRUKER_PROFILERING_TABELL = "BRUKER_PROFILERING";

    public ProfileringRepository(JdbcTemplate db) {
        this.db = db;
    }


    public void updateBrukerProfilering(ArbeidssokerProfilertEvent kafkaMelding) {
        Timestamp timestamp = Timestamp.from(ZonedDateTime.parse(kafkaMelding.getProfileringGjennomfort()).toInstant());
        SqlUtils.update(db, BRUKER_PROFILERING_TABELL)
                .set("PROFILERING_RESULTAT", kafkaMelding.getProfilertTil().name())
                .set("AKTOERID", kafkaMelding.getAktorid())
                .set("PROFILERING_TIDSPUNKT", timestamp)
                .whereEquals("AKTOERID", kafkaMelding.getAktorid())
                .execute();
    }

    public void insertBrukerProfilering (ArbeidssokerProfilertEvent kafkaMelding) {
        Timestamp timestamp = Timestamp.from(ZonedDateTime.parse(kafkaMelding.getProfileringGjennomfort()).toInstant());
        SqlUtils.insert(db, BRUKER_PROFILERING_TABELL)
                .value("PROFILERING_RESULTAT", kafkaMelding.getProfilertTil().name())
                .value("AKTOERID", kafkaMelding.getAktorid())
                .value("PROFILERING_TIDSPUNKT", timestamp)
                .execute();
    }

    public void insertProfileringFraArena (OppfolgingsBruker oppfolgingsBruker) {
        Timestamp timestamp = Timestamp.valueOf("1970-01-01 00:00:00.000");
        String profilering = oppfolgingsBruker.getKvalifiseringsgruppekode().equals("BKART") ? "OPPGITT_HINDRINGER" : "ANTATT_GODE_MULIGHETER";
        SqlUtils.insert(db, BRUKER_PROFILERING_TABELL)
                .value("PROFILERING_RESULTAT", profilering)
                .value("AKTOERID", oppfolgingsBruker.getAktoer_id())
                .value("PROFILERING_TIDSPUNKT", timestamp)
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
