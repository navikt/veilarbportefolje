package no.nav.fo.database;

import javaslang.collection.List;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;

import static no.nav.fo.util.DbUtils.mapResultSetTilDokument;

import static org.slf4j.LoggerFactory.getLogger;

public class BrukerRepository {

    private Logger LOG = getLogger(BrukerRepository.class);

    @Inject
    private JdbcTemplate db;

    private String dateFormat = "'YYYY-MM-DD HH24:MI:SS.FF'";

    public List<SolrInputDocument> retrieveAlleBrukere() {
        java.util.List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return List.ofAll(brukere);
    }

    public java.util.List<Map<String, Object>> retrieveOppdaterteBrukere() {
        java.util.List<Map<String, Object>> rader = db.queryForList(retrieveOppdaterteBrukereSQL());
        return rader;
    }
    public java.util.List<Map<String,Object>> retrieveBrukerSomHarVeileder(String personid) {
        return db.queryForList(retrieveBrukerSomHarVeilederSQL(),personid);
    }

    public int updateTidsstempel(Timestamp tidsstempel) {
        return db.update(updateTidsstempelSQL(), tidsstempel);
    }
    public java.util.List<Map<String,Object>> retrieveBruker(String aktoerid) {
        return db.queryForList(retrieveBrukerSQL(),aktoerid);
    }

    public java.util.List<Map<String,Object>> retrievePersonid(String aktoerid) {
        return db.queryForList(getPersonidFromAktoeridSQL(),aktoerid);
    }
    public java.util.List<Map<String,Object>> retrievePersonidFromFnr(String fnr) {
        return db.queryForList(getPersonIdFromFnrSQL(),fnr);
    }
    public void insertBrukerdata(String aktoerid, String personid, String veilederident, String tilordnetTidsstempel) throws DuplicateKeyException {
        db.update(insertBrukerdataSQL(), aktoerid, veilederident, tilordnetTidsstempel, personid);
    }
    public void updateBrukerdata(String aktoerid, String personid, String veilederident, String tilordnetTidsstempel) {
        db.update(updateBrukerdataSQL(),veilederident,tilordnetTidsstempel,personid,aktoerid);
    }

    public void insertOrUpdateBrukerdata(String aktoerid, String personid, String veilederident, String tilordnetTidsstempel) {
        try {
            insertBrukerdata(aktoerid, personid, veilederident, tilordnetTidsstempel);
        } catch(DuplicateKeyException e) {
            updateBrukerdata(aktoerid, personid, veilederident, tilordnetTidsstempel);
        }
    }

    public void insertAktoeridToPersonidMapping(String aktoerid, String personid) {
        try {
            db.update(insertPersonidAktoeridMappingSQL(),aktoerid,personid);
        } catch (DuplicateKeyException e) {
            LOG.info("Aktoerid %s personid %s mapping finnes i databasen", aktoerid, personid);
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

    String insertPersonidAktoeridMappingSQL() {
        return "INSERT INTO AKTOERID_TO_PERSONID VALUES (?,?)";
    }

    String insertBrukerdataSQL() {
        return "INSERT INTO BRUKER_DATA VALUES(?,?,TO_TIMESTAMP(?,"+dateFormat+"),?)";
    }

    String updateBrukerdataSQL() {
        return "UPDATE BRUKER_DATA" +
                "   SET VEILEDERIDENT=?," +
                "   TILDELT_TIDSPUNKT=TO_TIMESTAMP(?,"+dateFormat+")," +
                "   PERSONID=?" +
                "   WHERE AKTOERID=?";
    }

    String retrieveBrukerSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE AKTOERID=?";
    }
}
