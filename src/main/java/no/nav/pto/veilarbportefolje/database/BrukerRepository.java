package no.nav.pto.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.Table.OPPFOLGINGSBRUKER;
import no.nav.pto.veilarbportefolje.database.Table.OPPFOLGING_DATA;
import no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.UnderOppfolgingRegler;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.Table.AKTOERID_TO_PERSONID;
import static no.nav.pto.veilarbportefolje.database.Table.Kolonner.SIST_INDEKSERT_ES;
import static no.nav.pto.veilarbportefolje.database.Table.METADATA;
import static no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO.FODSELSNR;
import static no.nav.pto.veilarbportefolje.util.DbUtils.*;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.batchProcess;
import static no.nav.sbl.sql.SqlUtils.*;
import static no.nav.sbl.sql.where.WhereClause.gt;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
@Repository
public class BrukerRepository {

    private final JdbcTemplate db;

    @Autowired
    public BrukerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void oppdaterSistIndeksertElastic(Timestamp tidsstempel) {
        Integer oppdaterteRader = update(db, METADATA)
                .set(SIST_INDEKSERT_ES, tidsstempel)
                .execute();

        if (oppdaterteRader != 1) {
            throw new RuntimeException("Klarte ikke å oppdaterer tidsstempel for sist indeksert i elastic");
        }
    }

    public Optional<Fnr> hentFnrFraView(AktoerId aktoerId) {
        String fnr = select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> rs.getString(FODSELSNR))
                .column("*")
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();

        return Optional
                .ofNullable(fnr)
                .map(Fnr::of);
    }

    public List<OppfolgingsBruker> hentAlleBrukereUnderOppfolging() {
        db.setFetchSize(10_000);

        return SqlUtils
                .select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs) : null)
                .column("*")
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public List<OppfolgingsBruker> hentOppdaterteBrukere() {
        db.setFetchSize(1000);

        Timestamp sistIndeksert = SqlUtils
                .select(db, Table.METADATA, rs -> rs.getTimestamp(SIST_INDEKSERT_ES))
                .column(SIST_INDEKSERT_ES)
                .execute();

        return SqlUtils
                .select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> mapTilOppfolgingsBruker(rs))
                .column("*")
                .where(gt("TIDSSTEMPEL", sistIndeksert))
                .executeToList();
    }

    public Optional<OppfolgingsBruker> hentBrukerFraView(AktoerId aktoerId) {
        final OppfolgingsBruker bruker = select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> mapTilOppfolgingsBruker(rs))
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();

        return Optional.ofNullable(bruker);
    }

    public Optional<OppfolgingsBruker> hentBrukerFraView(Fnr fnr) {
        final OppfolgingsBruker bruker = select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> mapTilOppfolgingsBruker(rs))
                .column("*")
                .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                .execute();

        return Optional.ofNullable(bruker);
    }

    public List<OppfolgingsBruker> hentBrukereFraView(List<PersonId> personIds) {
        db.setFetchSize(1000);
        List<Integer> ids = personIds.stream().map(PersonId::toInteger).collect(toList());
        return SqlUtils
                .select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs) : null)
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

    public Optional<VeilederId> hentVeilederForBruker(AktoerId aktoerId) {
        VeilederId veilederId = select(db, OPPFOLGING_DATA.TABLE_NAME, this::mapToVeilederId)
                .column(OPPFOLGING_DATA.VEILEDERIDENT)
                .where(WhereClause.equals(OPPFOLGING_DATA.AKTOERID, aktoerId.toString()))
                .execute();

        return Optional.ofNullable(veilederId);
    }


    @Deprecated
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

    public Optional<String> hentNavKontorFraView(AktoerId aktoerId) {
        String navKontor = select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, this::mapToEnhet)
                .column(VW_PORTEFOLJE_INFO.NAV_KONTOR)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();

        return Optional.ofNullable(navKontor);
    }

    public Optional<String> hentNavKontorFraDbLinkTilArena(Fnr fnr) {
        String navKontor = select(db, OPPFOLGINGSBRUKER.TABLE_NAME, this::mapToEnhet)
                .column(OPPFOLGINGSBRUKER.NAV_KONTOR)
                .where(WhereClause.equals(FODSELSNR, fnr.toString()))
                .execute();

        return Optional.ofNullable(navKontor);
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

    public Integer insertGamleAktoerIdMedGjeldeneFlaggNull(AktoerId aktoerId, PersonId personId) {
        return insert(db, AKTOERID_TO_PERSONID.TABLE_NAME)
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .value("GJELDENE", 0)
                .execute();
    }

    @Transactional
    public void oppdaterPersonIdAktoerIdMapping(AktoerId aktoerId, PersonId personId) {
        update(db, AKTOERID_TO_PERSONID.TABLE_NAME)
                .set("GJELDENE", 0)
                .whereEquals("PERSONID", personId.toString())
                .execute();

        insert(db, AKTOERID_TO_PERSONID.TABLE_NAME)
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .value("GJELDENE", 1)
                .execute();
    }

    public Try<PersonId> retrievePersonid(AktoerId aktoerId) {
        return Try.of(
                () -> select(db, AKTOERID_TO_PERSONID.TABLE_NAME, this::mapToPersonIdFromAktoerIdToPersonId)
                        .column("PERSONID")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        )
                .onFailure(e -> log.warn("Fant ikke personid for aktoerid: " + aktoerId, e));
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

    public List<Brukerdata> retrieveBrukerdata(List<String> personIds) {
        return SqlUtils.select(db, Table.BRUKER_DATA.TABLE_NAME, BrukerRepository::toBrukerData)
                .column("*")
                .where(in(Table.BRUKER_DATA.PERSONID, personIds))
                .executeToList();
    }

    public Map<String, Optional<String>> retrievePersonidFromFnrs(Collection<String> fnrs) {
        Map<String, Optional<String>> brukere = new HashMap<>(fnrs.size());

        batchProcess(1000, fnrs, (fnrBatch) -> {

            final Map<String, Optional<String>> fnrPersonIdMap = select(db, OPPFOLGINGSBRUKER.TABLE_NAME, BrukerRepository::toFnrIdTuple)
                    .column("person_id")
                    .column("fodselsnr")
                    .where(in(OPPFOLGINGSBRUKER.FODSELSNR, fnrs))
                    .executeToList()
                    .stream()
                    .collect(Collectors.toMap(Tuple2::_1, personData -> Optional.of(personData._2())));

            brukere.putAll(fnrPersonIdMap);
        });

        fnrs.stream()
                .filter(not(brukere::containsKey))
                .forEach((ikkeFunnetBruker) -> brukere.put(ikkeFunnetBruker, empty()));

        return brukere;
    }

    @SneakyThrows
    private static Tuple2<String, String> toFnrIdTuple(ResultSet rs) {
        return Tuple.of(rs.getString(OPPFOLGINGSBRUKER.FODSELSNR), rs.getString(OPPFOLGINGSBRUKER.PERSON_ID));
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

    @SneakyThrows
    public static Brukerdata toBrukerData(ResultSet rs) {
        return new Brukerdata()
                .setAktoerid(rs.getString("AKTOERID"))
                .setPersonid(rs.getString("PERSONID"))
                .setYtelse(ytelsemappingOrNull(rs.getString("YTELSE")))
                .setUtlopsdato(toLocalDateTime(rs.getTimestamp("UTLOPSDATO")))
                .setUtlopsFasett(manedmappingOrNull(rs.getString("UTLOPSDATOFASETT")))
                .setDagputlopUke(rs.getInt("DAGPUTLOPUKE"))
                .setDagputlopUkeFasett(dagpengerUkeFasettMappingOrNull(rs.getString("DAGPUTLOPUKEFASETT")))
                .setPermutlopUke(rs.getInt("PERMUTLOPUKE"))
                .setPermutlopUkeFasett(dagpengerUkeFasettMappingOrNull(rs.getString("PERMUTLOPUKEFASETT")))
                .setAapmaxtidUke(rs.getInt("AAPMAXTIDUKE"))
                .setAapmaxtidUkeFasett(aapMaxtidUkeFasettMappingOrNull(rs.getString("AAPMAXTIDUKEFASETT")))
                .setAapUnntakDagerIgjen(rs.getInt("AAPUNNTAKDAGERIGJEN"))
                .setAapunntakUkerIgjenFasett(aapUnntakUkerIgjenFasettMappingOrNull(rs.getString("AAPUNNTAKUKERIGJENFASETT")))
                .setNyesteUtlopteAktivitet(rs.getTimestamp("NYESTEUTLOPTEAKTIVITET"))
                .setAktivitetStart(rs.getTimestamp("AKTIVITET_START"))
                .setNesteAktivitetStart(rs.getTimestamp("NESTE_AKTIVITET_START"))
                .setForrigeAktivitetStart(rs.getTimestamp("FORRIGE_AKTIVITET_START"));
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

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private static ManedFasettMapping manedmappingOrNull(String string) {
        return string != null ? ManedFasettMapping.valueOf(string) : null;
    }

    private static YtelseMapping ytelsemappingOrNull(String string) {
        return string != null ? YtelseMapping.valueOf(string) : null;
    }

    private static AAPMaxtidUkeFasettMapping aapMaxtidUkeFasettMappingOrNull(String string) {
        return string != null ? AAPMaxtidUkeFasettMapping.valueOf(string) : null;
    }

    private static AAPUnntakUkerIgjenFasettMapping aapUnntakUkerIgjenFasettMappingOrNull(String string) {
        return string != null ? AAPUnntakUkerIgjenFasettMapping.valueOf(string) : null;
    }

    private static DagpengerUkeFasettMapping dagpengerUkeFasettMappingOrNull(String string) {
        return string != null ? DagpengerUkeFasettMapping.valueOf(string) : null;
    }

}
