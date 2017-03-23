package no.nav.fo.database;

import javaslang.Tuple;
import javaslang.Tuple2;
import no.nav.fo.util.DbUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.DbUtils.mapResultSetTilDokument;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.batchProcess;
import static org.slf4j.LoggerFactory.getLogger;

public class BrukerRepository {

    static final private Logger LOG = getLogger(BrukerRepository.class);

    @Inject
    private JdbcTemplate db;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    static final private String dateFormat = "'YYYY-MM-DD HH24:MI:SS.FF'";

    public List<SolrInputDocument> retrieveAlleBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return brukere.stream().filter(this::erOppfolgingsBruker).collect(toList());
    }

    public List<SolrInputDocument> retrieveOppdaterteBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveOppdaterteBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return brukere.stream().filter(this::erOppfolgingsBruker).collect(toList());
    }

    public List<Map<String, Object>> retrieveBrukerSomHarVeileder(String personId) {
        return db.queryForList(retrieveBrukerSomHarVeilederSQL(), personId);
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

    public Optional<BigDecimal> retrievePersonidFromFnr(String fnr) {
        List<Map<String, Object>> list = db.queryForList(getPersonIdFromFnrSQL(), fnr);
        if (list.size() != 1) {
            LOG.error(format("Fikk %d antall rader for bruker med fnr %s", list.size(), fnr));
            return empty();
        }
        BigDecimal personId = (BigDecimal) list.get(0).get("PERSON_ID");
        return Optional.ofNullable(personId);
    }

    public Map<String, Optional<SolrInputDocument>> retrievePersonidFromFnrs(Collection<String> fnrs) {
        Map<String, Optional<SolrInputDocument>> brukere = new HashMap<>(fnrs.size());

        batchProcess(1000, fnrs, timed("GR199.brukersjekk.batch", (fnrBatch) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("fnrs", fnrBatch);

            Map<String, Optional<SolrInputDocument>> fnrPersonIdMap = namedParameterJdbcTemplate.queryForList(
                    getPersonIdsFromFnrsSQL(),
                    params)
                    .stream()
                    .map((rs) -> Tuple.of(
                            (String) rs.get("FODSELSNR"),
                            DbUtils.mapRadTilDokument(rs))
                    )
                    .collect(Collectors.toMap(Tuple2::_1, personData -> Optional.of(personData._2())));

            brukere.putAll(fnrPersonIdMap);
        }));

        fnrs.stream()
                .filter(not(brukere::containsKey))
                .forEach((ikkeFunnetBruker) -> brukere.put(ikkeFunnetBruker, empty()));

        return brukere;
    }


    private <T> Predicate<T> not(Predicate<T> predicate) {
        return (T t) -> !predicate.test(t);
    }

    public void insertBrukerdata(String aktoerId, String personId, String veilederident, String tilordnetTidsstempel) {
        db.update(insertBrukerdataSQL(), aktoerId, veilederident, tilordnetTidsstempel, personId);
    }

    public void updateBrukerdata(String aktoerId, String personId, String veilederident, String tilordnetTidsstempel) {
        db.update(updateBrukerdataSQL(), veilederident, tilordnetTidsstempel, personId, aktoerId);
    }

    public void insertOrUpdateBrukerdata(String aktoerId, String personId, String veilederident, String tilordnetTidsstempel) {
        try {
            insertBrukerdata(aktoerId, personId, veilederident, tilordnetTidsstempel);
        } catch (DuplicateKeyException e) {
            updateBrukerdata(aktoerId, personId, veilederident, tilordnetTidsstempel);
        }
    }

    public void insertAktoeridToPersonidMapping(String aktoerId, String personId) {
        try {
            db.update(insertPersonidAktoeridMappingSQL(), aktoerId, personId);
        } catch (DuplicateKeyException e) {
            LOG.info("Aktoerid {} personId {} mapping finnes i databasen", aktoerId, personId);
        }
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
                        "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS') || 'Z' AS iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS') || 'Z' AS doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "LEFT JOIN bruker_data " +
                        "ON " +
                        "bruker_data.personid = oppfolgingsbruker.person_id";

    }

    String retrieveBrukerSomHarVeilederSQL() {
        return
                "SELECT " +
                        "person_id, " +
                        "fodselsnr, " +
                        "fornavn, " +
                        "etternavn, " +
                        "nav_kontor, " +
                        "formidlingsgruppekode, " +
                        "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS') || 'Z' AS iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS') || 'Z' AS doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "LEFT JOIN bruker_data " +
                        "ON " +
                        "bruker_data.personid = oppfolgingsbruker.person_id " +
                        "WHERE " +
                        "person_id = ? ";
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
                        "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS') || 'Z' AS iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS') || 'Z' AS doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident " +
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
        return
                "UPDATE METADATA SET SIST_INDEKSERT = ?";
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
                        "fodselsnr, " +
                        "fornavn, " +
                        "etternavn, " +
                        "nav_kontor, " +
                        "formidlingsgruppekode, " +
                        "TO_CHAR(iserv_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(iserv_fra_dato, 'HH24:MI:SS') || 'Z' AS iserv_fra_dato, " +
                        "kvalifiseringsgruppekode, " +
                        "rettighetsgruppekode, " +
                        "hovedmaalkode, " +
                        "sikkerhetstiltak_type_kode, " +
                        "fr_kode, " +
                        "sperret_ansatt, " +
                        "er_doed, " +
                        "TO_CHAR(doed_fra_dato, 'YYYY-MM-DD') || 'T' || TO_CHAR(doed_fra_dato, 'HH24:MI:SS') || 'Z' AS doed_fra_dato, " +
                        "tidsstempel, " +
                        "veilederident " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "LEFT JOIN bruker_data " +
                        "ON " +
                        "bruker_data.personid = oppfolgingsbruker.person_id " +
                        "WHERE " +
                        "fodselsnr in (:fnrs)";
    }

    String insertPersonidAktoeridMappingSQL() {
        return "INSERT INTO AKTOERID_TO_PERSONID VALUES (?,?)";
    }

    String insertBrukerdataSQL() {
        return "INSERT INTO BRUKER_DATA VALUES(?,?,TO_TIMESTAMP(?," + dateFormat + "),?)";
    }

    String updateBrukerdataSQL() {
        return "UPDATE BRUKER_DATA" +
                "   SET VEILEDERIDENT=?," +
                "   TILDELT_TIDSPUNKT=TO_TIMESTAMP(?," + dateFormat + ")," +
                "   PERSONID=?" +
                "   WHERE AKTOERID=?";
    }

    String retrieveBrukerSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE AKTOERID=?";
    }

    private boolean erOppfolgingsBruker(SolrInputDocument bruker) {
        String innsatsgruppe = (String) bruker.get("kvalifiseringsgruppekode").getValue();

        boolean aktivStatus = !(bruker.get("formidlingsgruppekode").getValue().equals("ISERV") ||
                (bruker.get("formidlingsgruppekode").getValue().equals("IARBS") && (innsatsgruppe.equals("BKART")
                        || innsatsgruppe.equals("IVURD") || innsatsgruppe.equals("KAP11")
                        || innsatsgruppe.equals("VARIG") || innsatsgruppe.equals("VURDI"))));

        return aktivStatus || bruker.get("veileder_id").getValue() != null;
    }
}
