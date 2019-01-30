package no.nav.fo.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.BrukerDTO;
import no.nav.fo.veilarbportefolje.util.DbUtils;
import no.nav.fo.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.database.Tabell.AKTOERID_TO_PERSONID;
import static no.nav.fo.veilarbportefolje.database.Tabell.Kolonner.SIST_INDEKSERT_ES;
import static no.nav.fo.veilarbportefolje.database.Tabell.METADATA;
import static no.nav.fo.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static no.nav.fo.veilarbportefolje.util.DbUtils.*;
import static no.nav.fo.veilarbportefolje.util.StreamUtils.batchProcess;
import static no.nav.sbl.sql.SqlUtils.*;
import static no.nav.sbl.sql.where.WhereClause.gt;

@Slf4j
public class BrukerRepository {


    JdbcTemplate db;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Inject
    public BrukerRepository(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public int oppdaterSistIndeksertElastic(Timestamp tidsstempel) {
        return SqlUtils.update(db, Tabell.METADATA)
                .set(SIST_INDEKSERT_ES, tidsstempel)
                .execute();
    }

    public List<BrukerDTO> hentAlleBrukereUnderOppfolging() {
        db.setFetchSize(1000);

        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, rs -> erUnderOppfolging(rs) ? mapTilBrukerDTO(rs) : null)
                .column("*")
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public List<BrukerDTO> hentOppdaterteBrukere() {

        db.setFetchSize(1000);

        Timestamp sistIndeksert = SqlUtils
                .select(db, Tabell.METADATA, rs -> rs.getTimestamp(SIST_INDEKSERT_ES))
                .column(SIST_INDEKSERT_ES)
                .execute();

        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, DbUtils::mapTilBrukerDTO)
                .column("*")
                .where(gt("TIDSSTEMPEL", sistIndeksert))
                .executeToList();
    }

    public BrukerDTO hentBruker(AktoerId aktoerId) {
        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, DbUtils::mapTilBrukerDTO)
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();
    }

    public List<BrukerDTO> hentBrukere(List<PersonId> personIds) {
        db.setFetchSize(1000);
        List<Integer> ids = personIds.stream().map(PersonId::toInteger).collect(toList());

        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, rs -> erUnderOppfolging(rs) ? mapTilBrukerDTO(rs) : null)
                .column("*")
                .where(WhereClause.in("PERSON_ID", ids))
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public boolean erUnderOppfolging(ResultSet rs) {
        return harOppfolgingsFlaggSatt(rs) || erUnderOppfolgingIArena(rs);
    }

    @SneakyThrows
    private static boolean erUnderOppfolgingIArena(ResultSet rs) {
        String formidlingsgruppekode = rs.getString("formidlingsgruppekode");
        String kvalifiseringsgruppekode = rs.getString("kvalifiseringsgruppekode");
        return UnderOppfolgingRegler.erUnderOppfolging(formidlingsgruppekode, kvalifiseringsgruppekode);
    }

    @SneakyThrows
    private static boolean harOppfolgingsFlaggSatt(ResultSet rs) {
        return parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING");
    }

    static final String SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO =
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
                    "ny_for_veileder, " +
                    "utlopsdatofasett, " +
                    "dagputlopuke, dagputlopukefasett, " +
                    "permutlopuke, permutlopukefasett, " +
                    "aapmaxtiduke, aapmaxtidukefasett, " +
                    "aapunntakdagerigjen, aapunntakukerigjenfasett, " +
                    "oppfolging, " +
                    "venterpasvarfrabruker, " +
                    "venterpasvarfranav, " +
                    "nyesteutlopteaktivitet, " +
                    "aktivitet_start, " +
                    "neste_aktivitet_start, " +
                    "forrige_aktivitet_start, " +
                    "manuell, " +
                    "reservertikrr, " +
                    "ARBEIDSLISTE_AKTIV, " +
                    "ARBEIDSLISTE_KOMMENTAR, " +
                    "ARBEIDSLISTE_OVERSKRIFT, " +
                    "ARBEIDSLISTE_FRIST, " +
                    "ARBEIDSLISTE_ENDRET_AV, " +
                    "ARBEIDSLISTE_ENDRET_TID " +
                    "FROM " +
                    "vw_portefolje_info";


    public void updateMetadata(String name, Date date) {
        update(db, METADATA).set(name, date).execute();
    }

    public Try<VeilederId> retrieveVeileder(AktoerId aktoerId) {
        return Try.of(
                () -> {
                    return select(db, "OPPFOLGING_DATA", this::mapToVeilederId)
                            .column("VEILEDERIDENT")
                            .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                            .execute();
                }
        ).onFailure(e -> log.warn("Fant ikke veileder for bruker med aktoerId {}", aktoerId));
    }

    public Try<String> retrieveEnhet(Fnr fnr) {
        return Try.of(
                () -> {
                    return select(db, "OPPFOLGINGSBRUKER", this::mapToEnhet)
                            .column("NAV_KONTOR")
                            .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                            .execute();
                }
        ).onFailure(e -> log.warn("Fant ikke oppfølgingsenhet for bruker"));
    }

    public Try<String> retrieveEnhet(PersonId personId) {
        return Try.of(
                () -> {
                    return select(db, "OPPFOLGINGSBRUKER", this::mapToEnhet)
                            .column("NAV_KONTOR")
                            .where(WhereClause.equals("PERSON_ID", personId.toString()))
                            .execute();
                }
        ).onFailure(e -> log.warn("Fant ikke oppfølgingsenhet for bruker"));
    }

    public Integer insertAktoeridToPersonidMapping(AktoerId aktoerId, PersonId personId) {
        return insert(db, AKTOERID_TO_PERSONID)
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .value("GJELDENE", 1)
                .execute();

    }

    public Integer insertGamleAktoerIdMedGjeldeneFlaggNull(AktoerId aktoerId, PersonId personId) {
        return insert(db, AKTOERID_TO_PERSONID)
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .value("GJELDENE", 0)
                .execute();
    }

    public Integer setGjeldeneFlaggTilNull(PersonId personId) {
        return update(db, AKTOERID_TO_PERSONID)
                .set("GJELDENE", 0)
                .whereEquals("PERSONID", personId.toString())
                .execute();
    }

    public Try<PersonId> retrievePersonid(AktoerId aktoerId) {
        return Try.of(
                () -> select(db, AKTOERID_TO_PERSONID, this::mapToPersonIdFromAktoerIdToPersonId)
                        .column("PERSONID")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        ).onFailure(e -> log.warn("Fant ikke personid for aktoerid: " + aktoerId, e));
    }

    public Try<PersonId> retrievePersonidFromFnr(Fnr fnr) {
        return Try.of(() ->
                select(db, "OPPFOLGINGSBRUKER", this::mapPersonIdFromOppfolgingsbruker)
                        .column("PERSON_ID")
                        .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                        .execute()
        ).onFailure(e -> log.warn("Fant ikke personid for fnr: " + fnr, e));
    }

    public Try<Fnr> retrieveFnrFromPersonid(PersonId personId) {
        return Try.of(() ->
                select(db, "OPPFOLGINGSBRUKER", this::mapFnrFromOppfolgingsbruker)
                        .column("FODSELSNR")
                        .where(WhereClause.equals("PERSON_ID", personId.toString()))
                        .execute()
        ).onFailure(e -> log.warn("Fant ikke fnr for personid: " + personId, e));
    }

    /**
     * MAPPING-FUNKSJONER
     */
    @SneakyThrows
    private String mapToEnhet(ResultSet rs) {
        return rs.getString("NAV_KONTOR");
    }

    @SneakyThrows
    private VeilederId mapToVeilederId(ResultSet rs) {
        return VeilederId.of(rs.getString("VEILEDERIDENT"));
    }

    @SneakyThrows
    private PersonId mapToPersonIdFromAktoerIdToPersonId(ResultSet rs) {
        return PersonId.of(rs.getString("PERSONID"));
    }

    @SneakyThrows
    private PersonId mapPersonIdFromOppfolgingsbruker(ResultSet resultSet) {
        return PersonId.of(Integer.toString(resultSet.getBigDecimal("PERSON_ID").intValue()));
    }

    @SneakyThrows
    private Fnr mapFnrFromOppfolgingsbruker(ResultSet resultSet) {
        return Fnr.of(resultSet.getString("FODSELSNR"));
    }

    public void prosesserBrukere(Predicate<SolrInputDocument> filter, Consumer<SolrInputDocument> prosess) {
        prosesserBrukere(10000, filter, prosess);
    }

    void prosesserBrukere(int fetchSize, Predicate<SolrInputDocument> filter, Consumer<SolrInputDocument> prosess) {
        db.setFetchSize(fetchSize);
        String sql = SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO;
        db.query(sql, rs -> {
            SolrInputDocument brukerDokument = mapResultSetTilDokument(rs);
            if (filter.test(brukerDokument)) {
                prosess.accept(brukerDokument);
            }
        });
    }


    public List<SolrInputDocument> retrieveOppdaterteBrukere() {
        List<SolrInputDocument> brukere = new ArrayList<>();
        db.setFetchSize(10000);
        String sql = retrieveOppdaterteBrukereSQL();
        db.query(sql, rs -> {
            brukere.add(mapResultSetTilDokument(rs));
        });

        return brukere;
    }

    public List<SolrInputDocument> retrieveBrukeremedBrukerdata(List<PersonId> personIds) {
        List<SolrInputDocument> dokumenter = new ArrayList<>(personIds.size());
        io.vavr.collection.List.ofAll(personIds).sliding(1000, 1000)
                .forEach(personIdsBatch -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("personids", personIdsBatch.toJavaStream().map(PersonId::toString).collect(toList()));
                    String sql = retrieveBrukereMedBrukerdataSQL();
                    namedParameterJdbcTemplate.query(sql, params, rs -> {
                        dokumenter.add(mapResultSetTilDokument(rs));
                    });
                });
        return dokumenter;
    }

    public SolrInputDocument retrieveBrukermedBrukerdata(String personId) {
        String[] args = new String[]{personId};
        String sql = retrieveBrukerMedBrukerdataSQL();
        return db.query(sql, args, (rs) -> {
            if (rs.isBeforeFirst()) {
                rs.next();
            }
            return mapResultSetTilDokument(rs);
        });
    }

    public List<Brukerdata> retrieveBrukerdata(List<String> personIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("fnrs", personIds);
        String sql = retrieveBrukerdataSQL();
        return namedParameterJdbcTemplate.queryForList(sql, params)
                .stream()
                .map(data -> new Brukerdata()
                        .setAktoerid((String) data.get("AKTOERID"))
                        .setPersonid((String) data.get("PERSONID"))
                        .setYtelse(ytelsemappingOrNull((String) data.get("YTELSE")))
                        .setUtlopsdato(toLocalDateTime((Timestamp) data.get("UTLOPSDATO")))
                        .setUtlopsFasett(manedmappingOrNull((String) data.get("UTLOPSDATOFASETT")))
                        .setDagputlopUke(intValue(data.get("DAGPUTLOPUKE")))
                        .setDagputlopUkeFasett(dagpengerUkeFasettMappingOrNull((String) data.get("DAGPUTLOPUKEFASETT")))
                        .setPermutlopUke(intValue(data.get("PERMUTLOPUKE")))
                        .setPermutlopUkeFasett(dagpengerUkeFasettMappingOrNull((String) data.get("PERMUTLOPUKEFASETT")))
                        .setAapmaxtidUke(intValue(data.get("AAPMAXTIDUKE")))
                        .setAapmaxtidUkeFasett(aapMaxtidUkeFasettMappingOrNull((String) data.get("AAPMAXTIDUKEFASETT")))
                        .setAapUnntakDagerIgjen(intValue(data.get("AAPUNNTAKDAGERIGJEN")))
                        .setAapunntakUkerIgjenFasett(aapUnntakUkerIgjenFasettMappingOrNull((String) data.get("AAPUNNTAKUKERIGJENFASETT")))
                        .setNyesteUtlopteAktivitet((Timestamp) data.get("NYESTEUTLOPTEAKTIVITET"))
                        .setAktivitetStart((Timestamp) data.get("AKTIVITET_START"))
                        .setNesteAktivitetStart((Timestamp) data.get("NESTE_AKTIVITET_START"))
                        .setForrigeAktivitetStart((Timestamp) data.get("FORRIGE_AKTIVITET_START")))
                .collect(toList());
    }

    public int updateIndeksertTidsstempel(Timestamp tidsstempel) {
        String sql = updateSistIndeksertSQL();
        return db.update(sql, tidsstempel);
    }

    public Map<String, Optional<String>> retrievePersonidFromFnrs(Collection<String> fnrs) {
        Map<String, Optional<String>> brukere = new HashMap<>(fnrs.size());

        batchProcess(1000, fnrs, (fnrBatch) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("fnrs", fnrBatch);
            String sql = getPersonIdsFromFnrsSQL();
            Map<String, Optional<String>> fnrPersonIdMap = namedParameterJdbcTemplate.queryForList(
                    sql,
                    params)
                    .stream()
                    .map((rs) -> Tuple.of(
                            (String) rs.get("FODSELSNR"),
                            rs.get("PERSON_ID").toString())
                    )
                    .collect(Collectors.toMap(Tuple2::_1, personData -> Optional.of(personData._2())));

            brukere.putAll(fnrPersonIdMap);
        });

        fnrs.stream()
                .filter(not(brukere::containsKey))
                .forEach((ikkeFunnetBruker) -> brukere.put(ikkeFunnetBruker, empty()));

        return brukere;
    }

    public void setAktiviteterSistOppdatert(String sistOppdatert) {
        String sql = "UPDATE METADATA SET aktiviteter_sist_oppdatert = ?";
        db.update(sql, timestampFromISO8601(sistOppdatert));
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

    public void slettYtelsesdata() {
        update(db, "bruker_data")
                .set("ytelse", (Object) null)
                .set("utlopsdato", (Object) null)
                .set("utlopsdatoFasett", (Object) null)
                .set("dagputlopuke", (Object) null)
                .set("dagputlopukefasett", (Object) null)
                .set("permutlopuke", (Object) null)
                .set("permutlopukefasett", (Object) null)
                .set("aapmaxtiduke", (Object) null)
                .set("aapmaxtidukefasett", (Object) null)
                .set("aapunntakdagerigjen", (Object) null)
                .set("aapunntakukerigjenfasett", (Object) null)
                .execute();
    }

    private String retrieveBrukerMedBrukerdataSQL() {
        return SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO +
                " " +
                "WHERE " +
                "person_id = ?";
    }

    private String retrieveBrukereMedBrukerdataSQL() {
        return SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO +
                " " +
                "WHERE " +
                "person_id in (:personids)";
    }

    String retrieveOppdaterteBrukereSQL() {
        return SELECT_PORTEFOLJEINFO_FROM_VW_PORTEFOLJE_INFO +
                " " +
                "WHERE " +
                "tidsstempel > (" + retrieveSistIndeksertSQL() + ")";
    }

    String retrieveSistIndeksertSQL() {
        return "SELECT SIST_INDEKSERT FROM METADATA";
    }

    String updateSistIndeksertSQL() {
        return "UPDATE METADATA SET SIST_INDEKSERT = ?";
    }

    private String getPersonIdsFromFnrsSQL() {
        return
                "SELECT " +
                        "person_id, " +
                        "fodselsnr " +
                        "FROM " +
                        "OPPFOLGINGSBRUKER " +
                        "WHERE " +
                        "fodselsnr in (:fnrs)";
    }

    private String retrieveBrukerdataSQL() {
        return "SELECT * FROM BRUKER_DATA WHERE PERSONID in (:fnrs)";
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

    private static Integer intValue(Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private ManedFasettMapping manedmappingOrNull(String string) {
        return string != null ? ManedFasettMapping.valueOf(string) : null;
    }

    private YtelseMapping ytelsemappingOrNull(String string) {
        return string != null ? YtelseMapping.valueOf(string) : null;
    }

    private AAPMaxtidUkeFasettMapping aapMaxtidUkeFasettMappingOrNull(String string) {
        return string != null ? AAPMaxtidUkeFasettMapping.valueOf(string) : null;
    }

    private AAPUnntakUkerIgjenFasettMapping aapUnntakUkerIgjenFasettMappingOrNull(String string) {
        return string != null ? AAPUnntakUkerIgjenFasettMapping.valueOf(string) : null;
    }

    private DagpengerUkeFasettMapping dagpengerUkeFasettMappingOrNull(String string) {
        return string != null ? DagpengerUkeFasettMapping.valueOf(string) : null;
    }
}
