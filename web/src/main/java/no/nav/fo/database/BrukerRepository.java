package no.nav.fo.database;

import javaslang.Tuple;
import javaslang.Tuple2;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.KvartalMapping;
import no.nav.fo.domene.ManedMapping;
import no.nav.fo.domene.YtelseMapping;
import no.nav.fo.util.sql.SqlUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.DbUtils.mapResultSetTilDokument;
import static no.nav.fo.util.DbUtils.parseJaNei;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.batchProcess;
import static org.slf4j.LoggerFactory.getLogger;

public class BrukerRepository {

    static final private Logger LOG = getLogger(BrukerRepository.class);
    private static final String IARBS = "IARBS";

    @Inject
    private JdbcTemplate db;

    @Inject
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void prosesserBrukere(Predicate<SolrInputDocument> filter, Consumer<SolrInputDocument> prosess) {
        prosesserBrukere(10000, filter, prosess);
    }

    public void prosesserBrukere(int fetchSize, Predicate<SolrInputDocument> filter, Consumer<SolrInputDocument> prosess) {
        db.setFetchSize(fetchSize);
        db.query(retrieveBrukereSQL(), rs -> {
            SolrInputDocument bruker = mapResultSetTilDokument(rs);
            if (filter.test(bruker)) {
                prosess.accept(bruker);
            }
        });
    }

    public List<SolrInputDocument> retrieveOppdaterteBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        db.query(retrieveOppdaterteBrukereSQL(), rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });
        return brukere.stream().filter(BrukerRepository::erOppfolgingsBruker).collect(toList());
    }

    public List<Map<String, Object>> retrieveBrukermedBrukerdata(String personId) {
        return db.queryForList(retrieveBrukerMedBrukerdataSQL(), personId);
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
                        .setOppfolging(parseJaNei((String) data.get("OPPFOLGING"), "OPPFOLGING")))
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

    public Optional<String> retrievePersonIdFromAktoerId(String aktoerId) {
        List<Map<String, Object>> list = retrieveBruker(aktoerId);
        if (list.size() != 1) {
            LOG.warn(format("Fikk %d antall rader for bruker med aktoerId %s", list.size(), aktoerId));
            return empty();
        }
        return Optional.of((String)list.get(0).get("PERSON_ID"));
    }

    public Optional<BigDecimal> retrievePersonidFromFnr(String fnr) {
        List<Map<String, Object>> list = db.queryForList(getPersonIdFromFnrSQL(), fnr);
        if (list.size() != 1) {
            LOG.warn(format("Fikk %d antall rader for bruker med fnr %s", list.size(), fnr));
            return empty();
        }
        BigDecimal personId = (BigDecimal) list.get(0).get("PERSON_ID");
        return Optional.ofNullable(personId);
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
        try {
            brukerdata.toInsertQuery(db).execute();
        } catch (DuplicateKeyException e) {
            brukerdata.toUpdateQuery(db).execute();
        }
    }

    public void insertAktoeridToPersonidMapping(String aktoerId, String personId) {
        try {
            db.update(insertPersonidAktoeridMappingSQL(), aktoerId, personId);
        } catch (DuplicateKeyException e) {
            LOG.info("Aktoerid {} personId {} mapping finnes i databasen", aktoerId, personId);
        }
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
                        "veilederident, " +
                        "ytelse, " +
                        "TO_CHAR(utlopsdato, 'YYYY-MM-DD') || 'T' || TO_CHAR(utlopsdato, 'HH24:MI:SS') || 'Z' AS utlopsdato, " +
                        "utlopsdatofasett, " +
                        "TO_CHAR(aapmaxtid, 'YYYY-MM-DD') || 'T' || TO_CHAR(aapmaxtid, 'HH24:MI:SS') || 'Z' AS aapmaxtid, " +
                        "aapmaxtidfasett, " +
                        "oppfolging, " +
                        "venterpasvarfrabruker, " +
                        "venterpasvarfranav " +
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
                        "veilederident, " +
                        "ytelse," +
                        "TO_CHAR(utlopsdato, 'YYYY-MM-DD') || 'T' || TO_CHAR(utlopsdato, 'HH24:MI:SS') || 'Z' AS utlopsdato, " +
                        "utlopsdatofasett, " +
                        "TO_CHAR(aapmaxtid, 'YYYY-MM-DD') || 'T' || TO_CHAR(aapmaxtid, 'HH24:MI:SS') || 'Z' AS aapmaxtid, " +
                        "aapmaxtidfasett, " +
                        "oppfolging, " +
                        "venterpasvarfrabruker, " +
                        "venterpasvarfranav " +
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
                        "veilederident," +
                        "ytelse, " +
                        "TO_CHAR(utlopsdato, 'YYYY-MM-DD') || 'T' || TO_CHAR(utlopsdato, 'HH24:MI:SS') || 'Z' AS utlopsdato, " +
                        "utlopsdatofasett, " +
                        "TO_CHAR(aapmaxtid, 'YYYY-MM-DD') || 'T' || TO_CHAR(aapmaxtid, 'HH24:MI:SS') || 'Z' AS aapmaxtid, " +
                        "aapmaxtidfasett, " +
                        "oppfolging, " +
                        "venterpasvarfrabruker, " +
                        "venterpasvarfranav " +
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
                        "fodselsnr " +
                        "FROM " +
                        "oppfolgingsbruker " +
                        "WHERE " +
                        "fodselsnr in (:fnrs)";
    }

    String insertPersonidAktoeridMappingSQL() {
        return "INSERT INTO AKTOERID_TO_PERSONID VALUES (?,?)";
    }

    String retrieveBrukerSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE AKTOERID=?";
    }

    String retrieveBrukerdataSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE PERSONID in (:fnrs)";
    }

    public static boolean erOppfolgingsBruker(SolrInputDocument bruker) {
        String innsatsgruppe = (String) bruker.get("kvalifiseringsgruppekode").getValue();

        boolean aktivStatus = !(bruker.get("formidlingsgruppekode").getValue().equals("ISERV") ||
                (bruker.get("formidlingsgruppekode").getValue().equals("IARBS") && (innsatsgruppe.equals("BKART")
                        || innsatsgruppe.equals("IVURD") || innsatsgruppe.equals("KAP11")
                        || innsatsgruppe.equals("VARIG") || innsatsgruppe.equals("VURDI"))));

        return aktivStatus || bruker.get("veileder_id").getValue() != null || oppfolgingsFlaggSatt(bruker);
    }

    static boolean oppfolgingsFlaggSatt(SolrInputDocument bruker) {
        return (Boolean) bruker.get("oppfolging").getValue();
    }

    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
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


}
