package no.nav.fo.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Arbeidsliste;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.exception.FantIkkeAktoerIdException;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.fo.util.DateUtils.toZonedDateTime;
import static no.nav.fo.util.DbUtils.getCauseString;
import static no.nav.fo.util.DbUtils.not;
import static no.nav.fo.util.StreamUtils.batchProcess;
import static no.nav.fo.util.sql.SqlUtils.*;

@Slf4j
public class ArbeidslisteRepository {

    @Inject
    private JdbcTemplate db;

    @Inject
    private DataSource ds;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public static final String ARBEIDSLISTE = "ARBEIDSLISTE";

    public Try<Arbeidsliste> retrieveArbeidsliste(AktoerId aktoerId) {
        return Try.of(
                () -> select(ds, ARBEIDSLISTE, ArbeidslisteRepository::arbeidslisteMapper)
                        .column("*")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        );
    }

    public Map<AktoerId, Optional<Arbeidsliste>> retrieveArbeidsliste(List<AktoerId> aktoerIds) {
        Map<AktoerId, Optional<Arbeidsliste>> arbeidslisteMap = new HashMap<>(aktoerIds.size());
        String arbeidslisteSQL = "SELECT * FROM ARBEIDSLISTE WHERE AKTOERID IN(:aktoerids)";

        batchProcess(1000, aktoerIds, (aktoerIdsBatch) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("aktoerids", aktoerIdsBatch.stream().map(AktoerId::toString).collect(toList()));

            Map<AktoerId, Optional<Arbeidsliste>> arbeidslisteMapBatch =
                    namedParameterJdbcTemplate.queryForList(arbeidslisteSQL, params)
                            .stream()
                            .map((rs) -> Tuple.of(AktoerId.of((String) rs.get("AKTOERID")), arbeidslisteMapper(rs)))
                            .collect(toMap(Tuple2::_1, (tuple) -> Optional.of(tuple._2())));

            arbeidslisteMap.putAll(arbeidslisteMapBatch);
        });

        aktoerIds.stream()
                .filter(not(arbeidslisteMap::containsKey))
                .forEach(aktoerId -> arbeidslisteMap.put(aktoerId, empty()));

        return arbeidslisteMap;
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
        ).onFailure(e -> log.warn("Kunne ikke inserte arbeidsliste til db: {}", getCauseString(e)));
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
        ).onFailure(e -> log.warn("Kunne ikke oppdatere arbeidsliste i db: {}", getCauseString(e)));
    }

    public Try<AktoerId> deleteArbeidsliste(AktoerId aktoerID) {
        return Try.of(
                () -> {
                    delete(ds, ARBEIDSLISTE)
                            .where(WhereClause.equals("AKTOERID", aktoerID.toString()))
                            .execute();
                    return aktoerID;
                }
        )
                .onSuccess((aktoerid) -> log.info("Arbeidsliste for aktoerid {} slettet", aktoerid.toString()))
                .onFailure(e -> log.warn("Kunne ikke slette arbeidsliste fra db: {}", getCauseString(e)));
    }

    public void deleteArbeidslisteForAktoerids(List<AktoerId> aktoerIds) {
        io.vavr.collection.List.ofAll(aktoerIds).sliding(1000,1000)
                .forEach(aktoerIdsBatch -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("aktoerids", aktoerIdsBatch.toJavaStream().map(AktoerId::toString).collect(toList()));
                    namedParameterJdbcTemplate.update(deleteArbeidslisteSql(), params);
                });
    }

    @SneakyThrows
    private static Arbeidsliste arbeidslisteMapper(ResultSet rs) {
        return new Arbeidsliste(
                VeilederId.of(rs.getString("SIST_ENDRET_AV_VEILEDERIDENT")),
                toZonedDateTime(rs.getTimestamp("ENDRINGSTIDSPUNKT")),
                rs.getString("KOMMENTAR"),
                toZonedDateTime(rs.getTimestamp("FRIST")));
    }

    private static Arbeidsliste arbeidslisteMapper(Map<String, Object> rs) {
        return new Arbeidsliste(
                VeilederId.of((String) rs.get("SIST_ENDRET_AV_VEILEDERIDENT")),
                toZonedDateTime((Timestamp) rs.get("ENDRINGSTIDSPUNKT")),
                (String) rs.get("KOMMENTAR"),
                toZonedDateTime((Timestamp) rs.get("FRIST")));
    }

    private String deleteArbeidslisteSql() {
        return "delete from arbeidsliste where aktoerid in (:aktoerids)";
    }
}
