package no.nav.pto.veilarbportefolje.database;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.Table.OPPFOLGINGSBRUKER;
import no.nav.pto.veilarbportefolje.database.Table.OPPFOLGING_DATA;
import no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.database.Table.AKTOERID_TO_PERSONID;
import static no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.VW_PORTEFOLJE_INFO.FODSELSNR;
import static no.nav.pto.veilarbportefolje.util.DbUtils.mapTilOppfolgingsBruker;
import static no.nav.pto.veilarbportefolje.util.DbUtils.parseJaNei;
import static no.nav.sbl.sql.SqlUtils.insert;
import static no.nav.sbl.sql.SqlUtils.select;
import static no.nav.sbl.sql.SqlUtils.update;
import static no.nav.sbl.sql.SqlUtils.upsert;
import static no.nav.sbl.sql.where.WhereClause.in;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BrukerRepository {
    private final JdbcTemplate db;
    private final UnleashService unleashService;

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

    public Optional<OppfolgingsBruker> hentBrukerFraView(AktorId aktoerId) {
        final OppfolgingsBruker bruker = select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> mapTilOppfolgingsBruker(rs, unleashService))
                .column("*")
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();

        return Optional.ofNullable(bruker);
    }


    public List<OppfolgingsBruker> hentBrukereFraView(List<AktorId> aktorIds) {
        db.setFetchSize(1000);
        List<String> ids = aktorIds.stream().map(AktorId::get).collect(toList());
        return SqlUtils
                .select(db, VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> erUnderOppfolging(rs) ? mapTilOppfolgingsBruker(rs, unleashService) : null)
                .column("*")
                .where(in("AKTOERID", ids))
                .executeToList()
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @SneakyThrows
    public static boolean erUnderOppfolging(ResultSet rs) {
        return parseJaNei(rs.getString("OPPFOLGING"), "OPPFOLGING");
    }

    public Optional<VeilederId> hentVeilederForBruker(AktorId aktoerId) {
        VeilederId veilederId = select(db, OPPFOLGING_DATA.TABLE_NAME, this::mapToVeilederId)
                .column(OPPFOLGING_DATA.VEILEDERIDENT)
                .where(WhereClause.equals(OPPFOLGING_DATA.AKTOERID, aktoerId.toString()))
                .execute();

        return Optional.ofNullable(veilederId);
    }


    @Deprecated
    public Try<VeilederId> retrieveVeileder(AktorId aktoerId) {
        return Try.of(
                () -> select(db, "OPPFOLGING_DATA", this::mapToVeilederId)
                        .column("VEILEDERIDENT")
                        .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                        .execute()
        ).onFailure(e -> log.warn("Fant ikke veileder for bruker med aktoerId {}", aktoerId));
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
                () -> select(db, "OPPFOLGINGSBRUKER", this::mapToEnhet)
                        .column("NAV_KONTOR")
                        .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                        .execute()
        ).onFailure(e -> log.warn("Fant ikke oppfølgingsenhet for bruker"));
    }

    public void upsertAktoeridToPersonidMapping(AktorId aktoerId, PersonId personId) {
        upsert(db, AKTOERID_TO_PERSONID.TABLE_NAME)
                .set("AKTOERID", aktoerId.toString())
                .set("PERSONID", personId.toString())
                .set("GJELDENE", 1)
                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                .execute();

    }

    public Integer insertGamleAktorIdMedGjeldeneFlaggNull(AktorId aktoerId, PersonId personId) {
        return insert(db, AKTOERID_TO_PERSONID.TABLE_NAME)
                .value("AKTOERID", aktoerId.toString())
                .value("PERSONID", personId.toString())
                .value("GJELDENE", 0)
                .execute();
    }

    public Integer setGjeldeneFlaggTilNull(PersonId personId) {
        return update(db, AKTOERID_TO_PERSONID.TABLE_NAME)
                .set("GJELDENE", 0)
                .whereEquals("PERSONID", personId.toString())
                .execute();
    }

    public Optional<List<AktorId>> hentGamleAktorIder(PersonId personId) {
        return Optional.ofNullable(SqlUtils
                .select(db, AKTOERID_TO_PERSONID.TABLE_NAME, rs -> rs == null ? null : AktorId.of(rs.getString(AKTOERID_TO_PERSONID.AKTOERID)))
                .column(AKTOERID_TO_PERSONID.AKTOERID)
                .where(
                        WhereClause.equals(AKTOERID_TO_PERSONID.PERSONID, personId.getValue())
                                .and(
                                        WhereClause.equals(AKTOERID_TO_PERSONID.GJELDENE, 0))
                ).executeToList());
    }

    public Try<PersonId> retrievePersonid(AktorId aktoerId) {
        return Try.of(
                        () -> select(db, AKTOERID_TO_PERSONID.TABLE_NAME, this::mapToPersonIdFromAktorIdToPersonId)
                                .column("PERSONID")
                                .where(WhereClause.equals("AKTOERID", aktoerId.toString()))
                                .execute()
                )
                .onFailure(e -> log.warn("Fant ikke personid for aktoerid: " + aktoerId, e));
    }

    public Optional<PersonId> retrievePersonidFromFnr(Fnr fnr) {
        Optional<PersonId> personId = ofNullable(
                select(db, "OPPFOLGINGSBRUKER", this::mapPersonIdFromOppfolgingsbruker)
                        .column("PERSON_ID")
                        .where(WhereClause.equals("FODSELSNR", fnr.toString()))
                        .execute()
        );
        if (personId.isEmpty()) {
            log.warn("Fant ikke personid for fnr: " + fnr);
        }
        return personId;
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
        return rs.getString("VEILEDERIDENT") == null ? null : VeilederId.of(rs.getString("VEILEDERIDENT"));
    }

    @SneakyThrows
    private PersonId mapToPersonIdFromAktorIdToPersonId(ResultSet rs) {
        return PersonId.of(rs.getString("PERSONID"));
    }

    @SneakyThrows
    private PersonId mapPersonIdFromOppfolgingsbruker(ResultSet resultSet) {
        return PersonId.of(Integer.toString(resultSet.getBigDecimal("PERSON_ID").intValue()));
    }

    @SneakyThrows
    private Fnr mapFnrFromOppfolgingsbruker(ResultSet resultSet) {
        return Fnr.ofValidFnr(resultSet.getString("FODSELSNR"));
    }

    public List<PersonId> hentMappedePersonIder(AktorId aktorId) {
        final String sql = "SELECT PERSONID FROM AKTOERID_TO_PERSONID WHERE GJELDENE = 1 AND AKTOERID = ?";
        return db.queryForList(sql, String.class, aktorId.get())
                .stream()
                .map(PersonId::of)
                .toList();
    }

    String retrieveSistIndeksertSQL() {
        return "SELECT SIST_INDEKSERT FROM METADATA";
    }

    String updateSistIndeksertSQL() {
        return "UPDATE METADATA SET SIST_INDEKSERT = ?";
    }
}
