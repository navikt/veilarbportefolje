package no.nav.fo.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.exception.FantIkkeAktoerIdException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.util.sql.where.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static no.nav.fo.util.DateUtils.toZonedDateTime;
import static no.nav.fo.util.DbUtils.getCauseString;
import static no.nav.fo.util.sql.SqlUtils.*;

public class ArbeidslisteRepository {
    private static Logger LOG = LoggerFactory.getLogger(ArbeidslisteRepository.class);

    @Inject
    private JdbcTemplate db;

    @Inject
    private DataSource ds;

    public static final String ARBEIDSLISTE = "ARBEIDSLISTE";

    public Try<Arbeidsliste> retrieveArbeidsliste(AktoerId aktoerId) {
        return Try.of(
                () -> select(ds, ARBEIDSLISTE, ArbeidslisteRepository::arbeidslisteMapper)
                        .column("*")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        ).onFailure(e -> LOG.warn("Kunne ikke hente ut arbeidsliste fra db: {}", getCauseString(e)));
    }

    public Try<AktoerId> insertArbeidsliste(ArbeidslisteData data) {
        return Try.of(
                () -> {

                    AktoerId aktoerId = Optional
                            .ofNullable(data.getAktoerId())
                            .orElseThrow(() -> new FantIkkeAktoerIdException(data.getFnr()));

                    upsert(db, ARBEIDSLISTE)
                            .set("AKTOERID", aktoerId.toString())
                            .set("SIST_ENDRET_AV_VEILEDERIDENT", data.getVeilederId().toString())
                            .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                            .execute();
                    return data.getAktoerId();
                }
        ).onFailure(e -> LOG.warn("Kunne ikke inserte arbeidsliste til db: {}", getCauseString(e)));
    }


    public Try<AktoerId> updateArbeidsliste(ArbeidslisteData data) {
        return Try.of(
                () -> {
                    update(db, ARBEIDSLISTE)
                            .set("SIST_ENDRET_AV_VEILEDERIDENT", data.getVeilederId().toString())
                            .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .whereEquals("AKTOERID", data.getAktoerId().toString())
                            .execute();
                    return data.getAktoerId();
                }
        ).onFailure(e -> LOG.warn("Kunne ikke oppdatere arbeidsliste i db: {}", getCauseString(e)));
    }

    public Try<AktoerId> deleteArbeidsliste(AktoerId aktoerID) {
        return Try.of(
                () -> {
                    delete(ds, ARBEIDSLISTE)
                            .where(WhereClause.equals("AKTOERID", aktoerID.toString()))
                            .execute();
                    return aktoerID;
                }
        ).onFailure(e -> LOG.warn("Kunne ikke slette arbeidsliste fra db: {}", getCauseString(e)));
    }

    @SneakyThrows
    private static Arbeidsliste arbeidslisteMapper(ResultSet rs) {
        return new Arbeidsliste(
                new VeilederId(rs.getString("SIST_ENDRET_AV_VEILEDERIDENT")),
                toZonedDateTime(rs.getTimestamp("ENDRINGSTIDSPUNKT")),
                rs.getString("KOMMENTAR"),
                toZonedDateTime(rs.getTimestamp("FRIST")));
    }

}
