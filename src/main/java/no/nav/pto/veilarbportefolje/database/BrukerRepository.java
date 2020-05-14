package no.nav.pto.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Pair;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.Result;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static no.nav.common.utils.CollectionUtils.mapOf;
import static no.nav.pto.veilarbportefolje.database.Tabell.*;
import static no.nav.pto.veilarbportefolje.database.Tabell.Kolonner.SIST_INDEKSERT_ES;
import static no.nav.pto.veilarbportefolje.util.DbUtils.*;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.batchProcess;
import static no.nav.sbl.sql.SqlUtils.*;
import static no.nav.sbl.sql.where.WhereClause.gt;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
public class BrukerRepository {

    JdbcTemplate db;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Inject
    public BrukerRepository(JdbcTemplate db, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.db = db;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public void oppdaterSistIndeksertElastic(Timestamp tidsstempel) {
        Integer oppdaterteRader = update(db, METADATA)
                .set(SIST_INDEKSERT_ES, tidsstempel)
                .execute();

        if (oppdaterteRader != 1) {
            throw new RuntimeException("Klarte ikke å oppdaterer tidsstempel for sist indeksert i elastic");
        }
    }

    public List<OppfolgingsBruker> hentAlleBrukereUnderOppfolging() {
        db.setFetchSize(10_000);

        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, rs -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs) : null)
                .column("*")
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public List<OppfolgingsBruker> hentAlleBrukereUnderOppfolging(int fromExclusive, int toInclusive) {
        int fetchSize = 1000;
        db.setFetchSize(fetchSize);

        log.info("Henter ut fra {} til {}", fromExclusive, toInclusive);
        List<String> fnr = hentFnrFraOppfolgingBrukerTabell(fromExclusive, toInclusive);

        log.info("Hent ut {} fnr fra OPPFOLGINGSBRUKER", fnr.size());

        String sql = "SELECT * FROM"
                + " VW_PORTEFOLJE_INFO"
                + " WHERE FODSELSNR IN (:fnr)";

        List<OppfolgingsBruker> brukere = namedParameterJdbcTemplate.query(
                sql,
                mapOf(Pair.of("fnr", fnr)),
                (rs, rowNum) -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs) : null
        );

        log.info("Hentet ut {} brukere fra VW_PORTEFOLJE_INFO", brukere.size());

        return brukere.stream().filter(Objects::nonNull).collect(toList());
    }

    public List<String> hentFnrFraOppfolgingBrukerTabell(int fromExclusive, int toInclusive) {
        String sql = "SELECT FODSELSNR "
                + "FROM (SELECT "
                + "BRUKER.FODSELSNR, "
                + "rownum rn "
                + "FROM ( "
                + "SELECT * "
                + "FROM OPPFOLGINGSBRUKER "
                + "ORDER BY FODSELSNR "
                + ") "
                + "BRUKER "
                + "WHERE rownum <= :to "
                + ")"
                + "WHERE rn > :from ";

        Map<String, Integer> parameters = mapOf(
                Pair.of("from", fromExclusive),
                Pair.of("to", toInclusive)
        );

        return namedParameterJdbcTemplate.queryForList(sql, parameters, String.class);
    }

    public List<OppfolgingEnhetDTO> hentBrukereUnderOppfolging(int pageNumber, int pageSize) {
        int rowNum = pageNumber * pageSize;
        int offset = rowNum - pageSize;

        log.info("rowNum: {}, offset: {}, pageSize: {}", rowNum, offset, pageSize);

        return SqlUtils.select(db, VW_PORTEFOLJE_INFO, BrukerRepository::mapTilOppfolgingEnhetDTO)
                .column("AKTOERID")
                .column("FODSELSNR")
                .column("PERSON_ID")
                .column("NAV_KONTOR")
                .where(WhereClause.equals("FORMIDLINGSGRUPPEKODE", "ARBS")
                        .or(WhereClause.equals("OPPFOLGING", "J"))
                        .or(
                                WhereClause.equals("FORMIDLINGSGRUPPEKODE", "IARBS")
                                        .and(in("KVALIFISERINGSGRUPPEKODE", asList("BATT", "BFORM", "VARIG", "IKVAL", "VURDU", "OPPFI")))
                        )
                )
                .limit(offset, pageSize)
                .executeToList();
    }

    @SneakyThrows
    private static OppfolgingEnhetDTO mapTilOppfolgingEnhetDTO(ResultSet rs) {
        return new OppfolgingEnhetDTO(
                rs.getString("FODSELSNR"),
                rs.getString("AKTOERID"),
                rs.getString("NAV_KONTOR"),
                rs.getString("PERSON_ID")
        );
    }


    public Optional<Integer> hentAntallBrukereUnderOppfolging() {
        Integer count = db.query(countOppfolgingsBrukereSql(), rs -> {
            rs.next();
            return rs.getInt(1);
        });
        return ofNullable(count);
    }

    private String countOppfolgingsBrukereSql() {
        return "SELECT COUNT(*) FROM VW_PORTEFOLJE_INFO " +
                "WHERE FORMIDLINGSGRUPPEKODE = 'ARBS' " +
                "OR OPPFOLGING = 'J' " +
                "OR (FORMIDLINGSGRUPPEKODE = 'IARBS' AND KVALIFISERINGSGRUPPEKODE IN ('BATT', 'BFORM', 'VARIG', 'IKVAL', 'VURDU', 'OPPFI'))";
    }

    public List<OppfolgingsBruker> hentOppdaterteBrukere() {
        db.setFetchSize(1000);

        Timestamp sistIndeksert = SqlUtils
                .select(db, Tabell.METADATA, rs -> rs.getTimestamp(SIST_INDEKSERT_ES))
                .column(SIST_INDEKSERT_ES)
                .execute();

        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, rs -> mapTilOppfolgingsBruker(rs))
                .column("*")
                .where(gt("TIDSSTEMPEL", sistIndeksert))
                .executeToList();
    }

    public Result<OppfolgingsBruker> hentBruker(AktoerId aktoerId) {
        Supplier<OppfolgingsBruker> query = () -> SqlUtils.select(db, VW_PORTEFOLJE_INFO, rs -> mapTilOppfolgingsBruker(rs))
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();

        return Result.of(query);
    }

    public Result<OppfolgingsBruker> hentBruker(Fnr fnr) {
        Supplier<OppfolgingsBruker> query = () -> SqlUtils.select(db, VW_PORTEFOLJE_INFO, rs -> mapTilOppfolgingsBruker(rs))
                .column("*")
                .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                .execute();

        return Result.of(query);
    }

    public List<OppfolgingsBruker> hentBrukere(List<PersonId> personIds) {
        db.setFetchSize(1000);
        List<Integer> ids = personIds.stream().map(PersonId::toInteger).collect(toList());

        return SqlUtils
                .select(db, Tabell.VW_PORTEFOLJE_INFO, rs -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs) : null)
                .column("*")
                .where(in("PERSON_ID", ids))
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

    public Result<String> hentEnhetForBruker(AktoerId aktoerId) {
        Supplier<String> query = () -> {
            return select(db, VW_PORTEFOLJE_INFO, rs -> rs.getString("NAV_KONTOR"))
                    .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                    .execute();
        };

        return Result.of(query);
    }

    @Deprecated
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

    public Map<String, Optional<String>> retrievePersonidFromFnrs(Collection<String> fnrs) {
        Map<String, Optional<String>> brukere = new HashMap<>(fnrs.size());

        batchProcess(1000, fnrs, (fnrBatch) -> {
            Map<String, Object> params = new HashMap<>();
            params.put("fnrs", fnrBatch);
            String sql = getPersonIdsFromFnrsSQL();
            Map<String, Optional<String>> fnrPersonIdMap = namedParameterJdbcTemplate.queryForList(sql, params)
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

    public void setAktiviteterSistOppdatert(Timestamp sistOppdatert) {
        String sql = "UPDATE METADATA SET aktiviteter_sist_oppdatert = ?";
        db.update(sql, sistOppdatert);
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
