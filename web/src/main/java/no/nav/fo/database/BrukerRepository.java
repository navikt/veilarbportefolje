package no.nav.fo.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import no.nav.fo.domene.*;
import no.nav.fo.domene.aktivitet.AktivitetDTO;
import no.nav.fo.domene.aktivitet.AktivitetTyper;
import no.nav.fo.domene.aktivitet.AktoerAktiviteter;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.util.DbUtils;
import no.nav.fo.util.UnderOppfolgingRegler;
import no.nav.fo.util.sql.InsertQuery;
import no.nav.fo.util.sql.SqlUtils;
import no.nav.fo.util.sql.UpsertQuery;
import no.nav.fo.util.sql.where.WhereClause;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.*;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static no.nav.fo.util.DbUtils.*;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.batchProcess;
import static no.nav.fo.util.sql.SqlUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public class BrukerRepository {

    private static final Logger LOG = getLogger(BrukerRepository.class);
    public static final String OPPFOLGINGSBRUKER = "OPPFOLGINGSBRUKER";
    public static final String BRUKERDATA = "BRUKER_DATA";
    public static final String BRUKERSTATUS_AKTIVITETER = "BRUKERSTATUS_AKTIVITETER";
    private final String AKTOERID_TO_PERSONID = "AKTOERID_TO_PERSONID";
    private final String METADATA = "METADATA";
    public static final String FORMIDLINGSGRUPPEKODE = "formidlingsgruppekode";
    public static final String KVALIFISERINGSGRUPPEKODE = "kvalifiseringsgruppekode";


    @Inject
    JdbcTemplate db;

    @Inject
    private DataSource ds;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void updateMetadata(String name, Date date) {
        update(db,METADATA).set(name, date).execute();
    }

    public void slettAlleAktivitetstatus(String aktivitettype) { db.execute("DELETE FROM BRUKERSTATUS_AKTIVITETER WHERE AKTIVITETTYPE = '"+aktivitettype+"'");}

    public Try<Oppfolgingstatus> retrieveOppfolgingstatus(PersonId personId) {
        if(personId == null) {
            return Try.failure(new NullPointerException());
        }
        return Try.of(
                () -> db.query(retrieveOppfolgingstatusSql(), new String[]{personId.toString()}, this::mapToOppfolgingstatus)
        ).onFailure(e -> LOG.warn("Feil ved uthenting av Arena statuskoder for personid {}", personId, e));
    }

    public Try<VeilederId> retrieveVeileder(AktoerId aktoerId) {
        return Try.of(
                () -> select(ds, BRUKERDATA, this::mapToVeilederId)
                        .column("VEILEDERIDENT")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        ).onFailure(e -> LOG.warn("Fant ikke veileder for bruker med aktoerId {}", aktoerId));
    }

    public Try<String> retrieveEnhet(Fnr fnr) {
        return Try.of(
                () -> select(ds, OPPFOLGINGSBRUKER, this::mapToEnhet)
                        .column("NAV_KONTOR")
                        .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                        .execute()
        ).onFailure(e -> LOG.warn("Fant ikke oppfølgingsenhet for bruker med fnr {}", fnr));
    }

    public Try<Integer> insertAktoeridToPersonidMapping(AktoerId aktoerId, PersonId personId) {
        return
                Try.of(
                        () -> insert(db, AKTOERID_TO_PERSONID)
                                .value("AKTOERID", aktoerId.toString())
                                .value("PERSONID", personId.toString())
                                .execute()
                ).onFailure(e -> LOG.info("Kunne ikke inserte mapping Aktoerid {} -> personId {} i databasen: {}", aktoerId, personId, getCauseString(e)));
    }

    public Try<PersonId> retrievePersonid(AktoerId aktoerId) {
        return Try.of(
                () -> select(db.getDataSource(), AKTOERID_TO_PERSONID, this::mapToPersonId)
                        .column("PERSONID")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        ).onFailure(e -> LOG.warn("Fant ikke personid for aktoerid {}: {}", aktoerId, getCauseString(e)));
    }

    public Try<PersonId> retrievePersonidFromFnr(Fnr fnr) {
        return Try.of(() ->
                select(db.getDataSource(), OPPFOLGINGSBRUKER, this::personIdMapper)
                        .column("PERSON_ID")
                        .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                        .execute()
        ).onFailure(e -> LOG.warn("Fant ikke personid for fnr {}: {}", fnr, getCauseString(e)));
    }

    public Try<PersonId> deleteBrukerdata(PersonId personId) {
        return Try.of(
                () -> {
                    delete(db.getDataSource(),BRUKERDATA)
                            .where(WhereClause.equals("PERSONID", personId.toString()))
                            .execute();
                    return personId;
                }
        ).onFailure((e) -> LOG.warn("Kunne ikke slette brukerdata for personid {}",personId.toString(), e));
    }


    /**
     * MAPPING-FUNKSJONER
     */
    @SneakyThrows
    private Fnr fnrMapper(ResultSet rs) {
        return new Fnr(rs.getString("FNR"));
    }

    @SneakyThrows
    private String mapToEnhet(ResultSet rs) {
        return rs.getString("NAV_KONTOR");
    }

    @SneakyThrows
    private VeilederId mapToVeilederId(ResultSet rs) {
        return new VeilederId(rs.getString("VEILEDERIDENT"));
    }

    @SneakyThrows
    private Oppfolgingstatus mapToOppfolgingstatus(ResultSet rs) {
        if(rs.isBeforeFirst()) {
            rs.next();
        }
        return new Oppfolgingstatus()
                .setFormidlingsgruppekode(rs.getString(FORMIDLINGSGRUPPEKODE))
                .setServicegruppekode(rs.getString(KVALIFISERINGSGRUPPEKODE))
                .setVeileder(rs.getString("veilederident"))
                .setOppfolgingsbruker(parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING"));
    }

    @SneakyThrows
    private PersonId mapToPersonId(ResultSet rs) {
        return new PersonId(rs.getString("PERSONID"));
    }

    @SneakyThrows
    private PersonId personIdMapper(ResultSet resultSet) {
        return new PersonId(Integer.toString(resultSet.getBigDecimal("PERSON_ID").intValue()));
    }

    public Object prosesserBrukere(Predicate<SolrInputDocument> filter, Consumer<SolrInputDocument> prosess) {
        prosesserBrukere(10000, filter, prosess);
        return null;
    }

    public void prosesserBrukere(int fetchSize, Predicate<SolrInputDocument> filter, Consumer<SolrInputDocument> prosess) {
        db.setFetchSize(fetchSize);
        db.query(retrieveBrukereSQL(), rs -> {
            SolrInputDocument brukerDokument = mapResultSetTilDokument(rs);
            if (filter.test(brukerDokument)) {
                prosess.accept(brukerDokument);
            }
        });
    }


    public List<SolrInputDocument> retrieveOppdaterteBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveOppdaterteBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return brukere;
    }

    public SolrInputDocument retrieveBrukermedBrukerdata(String personId) {
        String[] args = new String[]{personId};
        return db.query(retrieveBrukerMedBrukerdataSQL(), args, (rs) -> {
            if (rs.isBeforeFirst()) {
                rs.next();
            }
            return mapResultSetTilDokument(rs);
        });
    }

    public List<Brukerdata> retrieveBrukerdata(List<String> personIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("fnrs", personIds);
        return namedParameterJdbcTemplate.queryForList(retrieveBrukerdataSQL(), params)
                .stream()
                .map(data -> new Brukerdata()
                        .setAktoerid((String) data.get("AKTOERID"))
                        .setVeileder((String) data.get("VEILEDERIDENT"))
                        .setPersonid((String) data.get("PERSONID"))
                        .setTildeltTidspunkt((Timestamp) data.get("TILDELT_TIDSPUNKT"))
                        .setUtlopsdato(toLocalDateTime((Timestamp) data.get("UTLOPSDATO")))
                        .setYtelse(ytelsemappingOrNull((String) data.get("YTELSE")))
                        .setAapMaxtid(toLocalDateTime((Timestamp) data.get("AAPMAXTID")))
                        .setAapMaxtidFasett(kvartalmappingOrNull((String) data.get("AAPMAXTIDFASETT")))
                        .setUtlopsdatoFasett(manedmappingOrNull((String) data.get("UTLOPSDATOFASETT")))
                        .setOppfolging(parseJaNei((String) data.get("OPPFOLGING"), "OPPFOLGING"))
                        .setVenterPaSvarFraBruker(toLocalDateTime((Timestamp) data.get("VENTERPASVARFRABRUKER")))
                        .setVenterPaSvarFraNav(toLocalDateTime((Timestamp) data.get("VENTERPASVARFRANAV")))
                        .setIAvtaltAktivitet(parse0OR1((String) data.get("IAVTALTAKTIVITET")))
                        .setNyesteUtlopteAktivitet((Timestamp) data.get("NYESTEUTLOPTEAKTIVITET")))
                .collect(toList());
    }

    public int updateTidsstempel(Timestamp tidsstempel) {
        return db.update(updateTidsstempelSQL(), tidsstempel);
    }

    public java.util.List<Map<String, Object>> retrieveBruker(String aktoerId) {
        return db.queryForList(retrieveBrukerSQL(), aktoerId);
    }

    public java.util.List<Map<String, Object>> retrievePersonid(String aktoerId) {
        return db.queryForList(getPersonidFromAktoeridSQL(), aktoerId);
    }


    public Map<String, Optional<String>> retrievePersonidFromFnrs(Collection<String> fnrs) {
        Map<String, Optional<String>> brukere = new HashMap<>(fnrs.size());

        batchProcess(1000, fnrs, timed("GR199.brukersjekk.batch", (fnrBatch) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("fnrs", fnrBatch);

            Map<String, Optional<String>> fnrPersonIdMap = namedParameterJdbcTemplate.queryForList(
                    getPersonIdsFromFnrsSQL(),
                    params)
                    .stream()
                    .map((rs) -> Tuple.of(
                            (String) rs.get("FODSELSNR"),
                            rs.get("PERSON_ID").toString())
                    )
                    .collect(Collectors.toMap(Tuple2::_1, personData -> Optional.of(personData._2())));

            brukere.putAll(fnrPersonIdMap);
        }));

        fnrs.stream()
                .filter(not(brukere::containsKey))
                .forEach((ikkeFunnetBruker) -> brukere.put(ikkeFunnetBruker, empty()));

        return brukere;
    }

    public Timestamp getAktiviteterSistOppdatert() {
        return (Timestamp) db.queryForList("SELECT aktiviteter_sist_oppdatert from METADATA").get(0).get("aktiviteter_sist_oppdatert");
    }

    public List<AktivitetDTO> getAktiviteterForAktoerid(AktoerId aktoerid) {
        return db.queryForList(getAktiviteterForAktoeridSql(), aktoerid.toString())
                .stream()
                .map(BrukerRepository::mapToAktivitetDTO)
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

    public void setAktiviteterSistOppdatert(String sistOppdatert) {
        db.update("UPDATE METADATA SET aktiviteter_sist_oppdatert = ?", timestampFromISO8601(sistOppdatert));
    }

    public void upsertAktivitet(AktivitetDataFraFeed aktivitet) {
        getAktivitetUpsertQuery(this.db, aktivitet).execute();
    }

    public void upsertAktivitet(Collection<AktivitetDataFraFeed> aktiviteter) {
        aktiviteter.forEach(this::upsertAktivitet);
    }


    public void upsertAktivitetStatus(AktivitetStatus a) {
        getUpsertAktivitetStatuserForBrukerQuery(a.getAktivitetType(), this.db, a.isAktiv(),
                a.getAktoerid().aktoerId, a.getPersonid().personId, a.getNesteUtlop())
                .execute();
    }

    public void insertAktivitetStatus(AktivitetStatus a) {
        getInsertAktivitetStatuserForBrukerQuery(a.getAktivitetType(), this.db, a.isAktiv(),
                a.getAktoerid().aktoerId, a.getPersonid().personId, a.getNesteUtlop())
                .execute();
    }

    public void insertOrUpdateAktivitetStatus(List<AktivitetStatus> aktivitetStatuses, Collection<Tuple2<PersonId,String>> finnesIdb) {
        Map<Boolean, List<AktivitetStatus>> eksisterendeStatuser = aktivitetStatuses
                .stream()
                .collect(groupingBy((data) -> finnesIdb.contains(Tuple.of(data.getPersonid(),data.getAktivitetType()))));

        AktivitetStatus.batchUpdate(this.db, eksisterendeStatuser.getOrDefault(true, emptyList()));

        eksisterendeStatuser.getOrDefault(false, emptyList())
                .forEach(this::upsertAktivitetStatus);
    }

    public Map<PersonId,Set<AktivitetStatus>> getAktivitetstatusForBrukere(Collection<PersonId> personIds) {

        Map<String, Object> params = new HashMap<>();
        params.put("personids", personIds.stream().map(PersonId::toString).collect(toList()));

        return namedParameterJdbcTemplate
                .queryForList(getAktivitetStatuserForListOfPersonIds(), params)
                .stream()
                .map( row -> AktivitetStatus.of(
                        new PersonId((String) row.get("PERSONID")),
                        new AktoerId((String) row.get("AKTOERID")),
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

    private <T> Predicate<T> not(Predicate<T> predicate) {
        return (T t) -> !predicate.test(t);
    }

    public void insertOrUpdateBrukerdata(List<Brukerdata> brukerdata, Collection<String> finnesIDb) {
        Map<Boolean, List<Brukerdata>> eksisterendeBrukere = brukerdata
                .stream()
                .collect(groupingBy((data) -> finnesIDb.contains(data.getPersonid())));


        Brukerdata.batchUpdate(db, eksisterendeBrukere.getOrDefault(true, emptyList()));

        eksisterendeBrukere
                .getOrDefault(false, emptyList())
                .forEach(this::upsertBrukerdata);
    }

    void upsertBrukerdata(Brukerdata brukerdata) {
        brukerdata.toUpsertQuery(db).execute();
    }

    public List<String> getBrukertiltak(String personId) {
        return db.queryForList(
            "SELECT " +
                "TILTAKSKODE AS TILTAK " +
                "FROM BRUKERTILTAK " +
                "WHERE PERSONID = ?", String.class, personId);
    }

    public void slettYtelsesdata() {
        SqlUtils.update(db, "bruker_data")
                .set("ytelse", null)
                .set("utlopsdato", null)
                .set("utlopsdatoFasett", null)
                .set("aapMaxtid", null)
                .set("aapMaxtidFasett", null)
                .execute();
    }

    static UpsertQuery getAktivitetUpsertQuery(JdbcTemplate db, AktivitetDataFraFeed aktivitet) {
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

    static UpsertQuery getUpsertAktivitetStatuserForBrukerQuery(String aktivitetstype, JdbcTemplate db, boolean status, String aktoerid, String personid, Timestamp nesteUtlopsdato) {
        return SqlUtils.upsert(db, BRUKERSTATUS_AKTIVITETER)
                .where(WhereClause.equals("PERSONID", personid).and(WhereClause.equals("AKTIVITETTYPE", aktivitetstype)))
                .set("STATUS", boolTo0OR1(status))
                .set("PERSONID", personid)
                .set("AKTIVITETTYPE", aktivitetstype)
                .set("AKTOERID", aktoerid)
                .set("NESTE_UTLOPSDATO", nesteUtlopsdato);
    }

    static InsertQuery getInsertAktivitetStatuserForBrukerQuery(String aktivitetstype, JdbcTemplate db, boolean status, String aktoerid, String personid, Timestamp nesteUtlopsdato) {
        return SqlUtils.insert(db, BRUKERSTATUS_AKTIVITETER)
                .value("STATUS", boolTo0OR1(status))
                .value("PERSONID", personid)
                .value("AKTIVITETTYPE", aktivitetstype)
                .value("AKTOERID", aktoerid)
                .value("NESTE_UTLOPSDATO", nesteUtlopsdato);
    }

    private String retrieveOppfolgingstatusSql() {
        return "SELECT " +
                "formidlingsgruppekode, " +
                "kvalifiseringsgruppekode, " +
                "bruker_data.oppfolging, " +
                "veilederident " +
                "FROM " +
                "oppfolgingsbruker " +
                "LEFT JOIN bruker_data " +
                "ON " +
                "bruker_data.personid = oppfolgingsbruker.person_id " +
                "WHERE " +
                "person_id = ? ";
    }

    String retrieveBrukereSQL() {
        return
                "SELECT " +
                        "person_id, " +
                        "fodselsnr, " +
                        "fornavn, " +
                        "etternavn, " +
                        "nav_kontor, " +
                        "formidlingsgruppekode, " +
                        "iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident, " +
                        "ytelse, " +
                        "utlopsdato, " +
                        "utlopsdatofasett, " +
                        "aapmaxtid, " +
                        "aapmaxtidfasett, " +
                        "oppfolging, " +
                        "venterpasvarfrabruker, " +
                        "venterpasvarfranav, " +
                        "nyesteutlopteaktivitet, " +
                        "iavtaltaktivitet " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "LEFT JOIN bruker_data " +
                        "ON " +
                        "bruker_data.personid = oppfolgingsbruker.person_id";

    }

    String retrieveBrukerMedBrukerdataSQL() {
        return
                "SELECT " +
                        "person_id, " +
                        "fodselsnr, " +
                        "fornavn, " +
                        "etternavn, " +
                        "nav_kontor, " +
                        "formidlingsgruppekode, " +
                        "iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident, " +
                        "ytelse," +
                        "utlopsdato, " +
                        "utlopsdatofasett, " +
                        "aapmaxtid, " +
                        "aapmaxtidfasett, " +
                        "oppfolging, " +
                        "venterpasvarfrabruker, " +
                        "venterpasvarfranav, " +
                        "nyesteutlopteaktivitet, " +
                        "iavtaltaktivitet " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "LEFT JOIN bruker_data " +
                        "ON " +
                        "bruker_data.personid = oppfolgingsbruker.person_id " +
                        "WHERE " +
                        "person_id = ?";
    }

    String retrieveOppdaterteBrukereSQL() {
        return
                "SELECT " +
                        "person_id, " +
                        "fodselsnr, " +
                        "fornavn, " +
                        "etternavn, " +
                        "nav_kontor, " +
                        "formidlingsgruppekode, " +
                        "iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident," +
                        "ytelse, " +
                        "utlopsdato, " +
                        "utlopsdatofasett, " +
                        "aapmaxtid, " +
                        "aapmaxtidfasett, " +
                        "oppfolging, " +
                        "venterpasvarfrabruker, " +
                        "venterpasvarfranav, " +
                        "nyesteutlopteaktivitet, " +
                        "iavtaltaktivitet " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "LEFT JOIN bruker_data " +
                        "ON " +
                        "bruker_data.personid = oppfolgingsbruker.person_id " +
                        "WHERE " +
                        "tidsstempel > (" + retrieveSistIndeksertSQL() + ")";
    }

    String retrieveSistIndeksertSQL() {
        return "SELECT SIST_INDEKSERT FROM METADATA";
    }

    String updateTidsstempelSQL() {
        return "UPDATE METADATA SET SIST_INDEKSERT = ?";
    }

    String getPersonidFromAktoeridSQL() {
        return "SELECT PERSONID FROM AKTOERID_TO_PERSONID WHERE AKTOERID = ?";
    }

    String getPersonIdFromFnrSQL() {
        return "SELECT PERSON_ID FROM OPPFOLGINGSBRUKER WHERE FODSELSNR= ?";
    }

    String getPersonIdsFromFnrsSQL() {
        return
                "SELECT " +
                        "person_id, " +
                        "fodselsnr " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "WHERE " +
                        "fodselsnr in (:fnrs)";
    }

    private String retrieveBrukerSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE AKTOERID=?";
    }

    private String retrieveBrukerdataSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE PERSONID in (:fnrs)";
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

    public static boolean erOppfolgingsBruker(SolrInputDocument bruker) {
        return oppfolgingsFlaggSatt(bruker) || erOppfolgingsBrukerIarena(bruker);
    }

    private static boolean erOppfolgingsBrukerIarena(SolrInputDocument bruker) {
        String servicegruppekode = (String) bruker.get("kvalifiseringsgruppekode").getValue();
        String formidlingsgruppekode = (String) bruker.get("formidlingsgruppekode").getValue();
        return UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, servicegruppekode);
    }

    static boolean oppfolgingsFlaggSatt(SolrInputDocument bruker) {
        return (Boolean) bruker.get("oppfolging").getValue();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private ManedMapping manedmappingOrNull(String string) {
        return string != null ? ManedMapping.valueOf(string) : null;
    }

    private YtelseMapping ytelsemappingOrNull(String string) {
        return string != null ? YtelseMapping.valueOf(string) : null;
    }

    private KvartalMapping kvartalmappingOrNull(String string) {
        return string != null ? KvartalMapping.valueOf(string) : null;
    }

    private Boolean kanskjeVerdi(List<Map<String, Object>> statuserFraDb, String type) {
        for (Map<String, Object> rad : statuserFraDb) {
            String aktivitetType = (String) rad.get("AKTIVITETTYPE");
            if (type.equals(aktivitetType)) {
                //med hsql driveren settes det inn false/true og med oracle settes det inn 0/1.
                return Boolean.valueOf((String) rad.get("STATUS")) || "1".equals(rad.get("STATUS"));
            }
        }
        return false;
    }
}
