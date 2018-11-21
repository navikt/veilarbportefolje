package no.nav.fo.veilarbportefolje.database;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.Arbeidsliste;
import no.nav.fo.veilarbportefolje.domene.VeilederId;
import no.nav.fo.veilarbportefolje.exception.FantIkkeAktoerIdException;
import no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static no.nav.fo.veilarbportefolje.util.DateUtils.toZonedDateTime;
import static no.nav.fo.veilarbportefolje.util.DbUtils.dbTimerNavn;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;
import static no.nav.sbl.sql.SqlUtils.*;

@Slf4j
public class ArbeidslisteRepository {

    private static final String DELETE_FROM_ARBEIDSLISTE_SQL = "delete from arbeidsliste where aktoerid = :aktoerid";

    @Inject
    private JdbcTemplate db;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public static final String ARBEIDSLISTE = "ARBEIDSLISTE";

    public Try<Arbeidsliste> retrieveArbeidsliste(AktoerId aktoerId) {
        return Try.of(
                () -> select(db, ARBEIDSLISTE, ArbeidslisteRepository::arbeidslisteMapper)
                        .column("*")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        );
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
                            .set("OVERSKRIFT", data.getOverskrift())
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                            .execute();
                    return data.getAktoerId();
                }
        ).onFailure(e -> log.warn("Kunne ikke inserte arbeidsliste til db", e));
    }


    public Try<AktoerId> updateArbeidsliste(ArbeidslisteData data) {
        return Try.of(
                () -> {
                    update(db, ARBEIDSLISTE)
                            .set("SIST_ENDRET_AV_VEILEDERIDENT", data.getVeilederId().toString())
                            .set("ENDRINGSTIDSPUNKT", Timestamp.from(Instant.now()))
                            .set("OVERSKRIFT", data.getOverskrift())
                            .set("KOMMENTAR", data.getKommentar())
                            .set("FRIST", data.getFrist())
                            .whereEquals("AKTOERID", data.getAktoerId().toString())
                            .execute();
                    return data.getAktoerId();
                }
        ).onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste i db", e));
    }

    public Try<AktoerId> deleteArbeidsliste(AktoerId aktoerID) {
        return Try.of(
                () -> {
                    delete(db, ARBEIDSLISTE)
                            .where(WhereClause.equals("AKTOERID", aktoerID.toString()))
                            .execute();
                    return aktoerID;
                }
        )
                .onSuccess((aktoerid) -> log.info("Arbeidsliste for aktoerid {} slettet", aktoerid.toString()))
                .onFailure(e -> log.warn("Kunne ikke slette arbeidsliste fra db", e));
    }

    public void deleteArbeidslisteForAktoerid(AktoerId aktoerId) {
        timed(dbTimerNavn(
                DELETE_FROM_ARBEIDSLISTE_SQL),
                ()-> namedParameterJdbcTemplate.update(DELETE_FROM_ARBEIDSLISTE_SQL, Collections.singletonMap("aktoerid", aktoerId.toString())));
    }

    @SneakyThrows
    private static Arbeidsliste arbeidslisteMapper(ResultSet rs) {
        return new Arbeidsliste(
                VeilederId.of(rs.getString("SIST_ENDRET_AV_VEILEDERIDENT")),
                toZonedDateTime(rs.getTimestamp("ENDRINGSTIDSPUNKT")),
                rs.getString("OVERSKRIFT"),
                rs.getString("KOMMENTAR"),
                toZonedDateTime(rs.getTimestamp("FRIST")));
    }

}
