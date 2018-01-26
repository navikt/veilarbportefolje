package no.nav.fo.aktivitet;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktivitetTyper;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.util.DbUtils;
import no.nav.fo.util.sql.InsertQuery;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.UpsertQuery;
import no.nav.fo.util.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static no.nav.fo.util.DbUtils.boolTo0OR1;
import static no.nav.fo.util.DbUtils.parse0OR1;

@Slf4j
public class AktivitetDAO {

    private static final String BRUKERSTATUS_AKTIVITETER = "BRUKERSTATUS_AKTIVITETER";
    private static final String AKTIVITETER = "AKTIVITETER";
    private static final String AKTIVITETID = "AKTIVITETID";

    private JdbcTemplate db;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private DataSource ds;

    public AktivitetDAO(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource ds) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.ds = ds;
    }


    public void slettAlleAktivitetstatus(String aktivitettype) {
        db.execute("DELETE FROM BRUKERSTATUS_AKTIVITETER WHERE AKTIVITETTYPE = '" + aktivitettype + "'");
    }

    public Timestamp getAktiviteterSistOppdatert() {
        return (Timestamp) db.queryForList("SELECT aktiviteter_sist_oppdatert from METADATA").get(0).get("aktiviteter_sist_oppdatert");
    }

    public List<AktivitetDTO> getAktiviteterForAktoerid(AktoerId aktoerid) {
        return db.queryForList(getAktiviteterForAktoeridSql(), aktoerid.toString())
            .stream()
            .map(AktivitetDAO::mapToAktivitetDTO)
            .filter(aktivitet -> AktivitetTyper.contains(aktivitet.getAktivitetType()))
            .collect(toList());
    }

    public List<AktoerAktiviteter> getAktiviteterForListOfAktoerid(Collection<String> aktoerids) {
        if (aktoerids.isEmpty()) {
            return emptyList();
        }

        Map<String, Object> params = new HashMap<>();
        Map<String, List<AktivitetDTO>> aktoerTilAktiviteterMap = new HashMap<>(aktoerids.size());
        List<AktoerAktiviteter> aktoerAktiviteter = new ArrayList<>(aktoerids.size());

        params.put("aktoerids", aktoerids);
        List<Map<String, Object>> queryResult = namedParameterJdbcTemplate.queryForList(getAktiviteterForAktoeridsSql(), params);

        queryResult.forEach(aktivitet -> {
            String aktoerid = (String) aktivitet.get("AKTOERID");
            if (aktoerTilAktiviteterMap.containsKey(aktoerid)) {
                aktoerTilAktiviteterMap.get(aktoerid).add(mapToAktivitetDTO(aktivitet));
            } else {
                List<AktivitetDTO> liste = new ArrayList<>();
                liste.add(mapToAktivitetDTO(aktivitet));
                aktoerTilAktiviteterMap.put(aktoerid, liste);
            }
        });

        aktoerids.forEach( aktoerid -> {
            if(!aktoerTilAktiviteterMap.containsKey(aktoerid)) {
                aktoerTilAktiviteterMap.put(aktoerid, new ArrayList<>());
            }
        });

        aktoerTilAktiviteterMap.forEach((key, value) -> aktoerAktiviteter.add(new AktoerAktiviteter(key).setAktiviteter(value)));

        return aktoerAktiviteter;
    }

    private static AktivitetDTO mapToAktivitetDTO(Map<String, Object> map) {
        return new AktivitetDTO()
            .setAktivitetType((String) map.get("AKTIVITETTYPE"))
            .setStatus((String) map.get("STATUS"))
            .setFraDato((Timestamp) map.get("FRADATO"))
            .setTilDato((Timestamp) map.get("TILDATO"));
    }

    public void upsertAktivitet(AktivitetDataFraFeed aktivitet) {
        getAktivitetUpsertQuery(this.db, aktivitet).execute();
    }

    public void deleteById(String aktivitetid) {
        log.info("Sletter alle aktiviteter med id {}", aktivitetid);
        SqlUtils.delete(ds, AKTIVITETER)
                .where(WhereClause.equals(AKTIVITETID, aktivitetid))
                .execute();
    }

    void upsertAktivitet(Collection<AktivitetDataFraFeed> aktiviteter) {
        aktiviteter.forEach(this::upsertAktivitet);
    }


    public void upsertAktivitetStatus(AktivitetStatus a) {
        getUpsertAktivitetStatuserForBrukerQuery(a.getAktivitetType(), this.db, a.isAktiv(),
            a.getAktoerid().aktoerId, a.getPersonid().personId, a.getNesteUtlop())
            .execute();
    }

    public void insertAktivitetstatuser(List<AktivitetStatus> statuser) {
        io.vavr.collection.List.ofAll(statuser).sliding(1000,1000)
                .forEach((statuserBatch) -> AktivitetStatus.batchInsert(db, statuserBatch.toJavaList()));
    }

    public void insertOrUpdateAktivitetStatus(List<AktivitetStatus> aktivitetStatuses, Collection<Tuple2<PersonId, String>> finnesIdb) {
        Map<Boolean, List<AktivitetStatus>> eksisterendeStatuser = aktivitetStatuses
            .stream()
            .collect(groupingBy((data) -> finnesIdb.contains(Tuple.of(data.getPersonid(), data.getAktivitetType()))));

        AktivitetStatus.batchUpdate(this.db, eksisterendeStatuser.getOrDefault(true, emptyList()));

        insertAktivitetstatuser(eksisterendeStatuser.getOrDefault(false, emptyList()));
    }

    public Map<PersonId, Set<AktivitetStatus>> getAktivitetstatusForBrukere(Collection<PersonId> personIds) {

        Map<String, Object> params = new HashMap<>();
        params.put("personids", personIds.stream().map(PersonId::toString).collect(toList()));

        return namedParameterJdbcTemplate
            .queryForList(getAktivitetStatuserForListOfPersonIds(), params)
            .stream()
            .map(row -> AktivitetStatus.of(
                PersonId.of((String) row.get("PERSONID")),
                AktoerId.of((String) row.get("AKTOERID")),
                (String) row.get("AKTIVITETTYPE"),
                parse0OR1((String) row.get("STATUS")),
                (Timestamp) row.get("NESTE_UTLOPSDATO"))
            )
            .filter(aktivitetStatus -> AktivitetTyper.contains(aktivitetStatus.getAktivitetType()))
            .collect(toMap(AktivitetStatus::getPersonid, DbUtils::toSet,
                (oldValue, newValue) -> {
                    oldValue.addAll(newValue);
                    return oldValue;
                }));
    }

    public List<String> getDistinctAktoerIdsFromAktivitet() {
        return db.queryForList("SELECT DISTINCT AKTOERID FROM AKTIVITETER")
            .stream()
            .map(map -> (String) map.get("AKTOERID"))
            .collect(toList());

    }

    public List<Brukertiltak> hentBrukertiltak(List<Fnr> fnrs) {

        Map<String, Object> params = new HashMap<>();
        params.put("fnrs", fnrs.stream().map(Fnr::toString).collect(toList()));

        return namedParameterJdbcTemplate
            .queryForList(hentBrukertiltakForListeAvFnrSQL(), params)
            .stream()
            .map(row -> Brukertiltak.of(
                Fnr.of((String) row.get("FNR")),
                (String) row.get("TILTAK"),
                (Timestamp) row.get("TILDATO"))
            )
            .collect(toList());
    }

    private static UpsertQuery getAktivitetUpsertQuery(JdbcTemplate db, AktivitetDataFraFeed aktivitet) {
        return SqlUtils.upsert(db, "AKTIVITETER")
            .where(WhereClause.equals("AKTIVITETID", aktivitet.getAktivitetId()))
            .set("AKTOERID", aktivitet.getAktorId())
            .set("AKTIVITETTYPE", aktivitet.getAktivitetType().toLowerCase())
            .set("AVTALT", aktivitet.isAvtalt())
            .set("FRADATO", aktivitet.getFraDato())
            .set("TILDATO", aktivitet.getTilDato())
            .set("OPPDATERTDATO", aktivitet.getEndretDato())
            .set("STATUS", aktivitet.getStatus().toLowerCase())
            .set("AKTIVITETID", aktivitet.getAktivitetId());
    }

    private static UpsertQuery getUpsertAktivitetStatuserForBrukerQuery(String aktivitetstype, JdbcTemplate db, boolean status, String aktoerid, String personid, Timestamp nesteUtlopsdato) {
        return SqlUtils.upsert(db, BRUKERSTATUS_AKTIVITETER)
            .where(WhereClause.equals("PERSONID", personid).and(WhereClause.equals("AKTIVITETTYPE", aktivitetstype)))
            .set("STATUS", boolTo0OR1(status))
            .set("PERSONID", personid)
            .set("AKTIVITETTYPE", aktivitetstype)
            .set("AKTOERID", aktoerid)
            .set("NESTE_UTLOPSDATO", nesteUtlopsdato);
    }

    private static InsertQuery getInsertAktivitetStatuserForBrukerQuery(String aktivitetstype, JdbcTemplate db, boolean status, String aktoerid, String personid, Timestamp nesteUtlopsdato) {
        return SqlUtils.insert(db, BRUKERSTATUS_AKTIVITETER)
            .value("STATUS", boolTo0OR1(status))
            .value("PERSONID", personid)
            .value("AKTIVITETTYPE", aktivitetstype)
            .value("AKTOERID", aktoerid)
            .value("NESTE_UTLOPSDATO", nesteUtlopsdato);
    }

    public void slettutlopsdatoForAktivitet() {
        SqlUtils.update(db, "bruker_data")
                .set("NYESTEUTLOPTEAKTIVITET", null)
                .set("AKTIVITET_START", null)
                .set("NESTE_AKTIVITET_START", null)
                .set("FORRIGE_AKTIVITET_START", null)
                .execute();
    }

    private String hentBrukertiltakForListeAvFnrSQL() {
        return "SELECT " +
            "TILTAKSKODE AS TILTAK, " +
            "FODSELSNR as FNR, " +
            "TILDATO " +
            "FROM BRUKERTILTAK " +
            "WHERE FODSELSNR in(:fnrs)";
    }

    private String getAktiviteterForAktoeridSql() {
        return "SELECT AKTIVITETTYPE, STATUS, FRADATO, TILDATO FROM AKTIVITETER where aktoerid=?";
    }

    private String getAktiviteterForAktoeridsSql() {
        return
            "SELECT " +
                "AKTOERID, " +
                "AKTIVITETTYPE, " +
                "STATUS, " +
                "FRADATO, " +
                "TILDATO " +
                "FROM " +
                "AKTIVITETER " +
                "WHERE " +
                "AKTOERID in (:aktoerids)";
    }

    private String getAktivitetStatuserForListOfPersonIds() {
        return
            "SELECT " +
                "PERSONID, " +
                "AKTOERID, " +
                "AKTIVITETTYPE, " +
                "STATUS, " +
                "NESTE_UTLOPSDATO " +
                "FROM " +
                "BRUKERSTATUS_AKTIVITETER " +
                "WHERE " +
                "PERSONID in (:personids)";
    }
}
