package no.nav.pto.veilarbportefolje.util;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepositoryV2;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.ZonedDateTime;

import static no.nav.pto.veilarbportefolje.database.Table.ARBEIDSLISTE.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE;
import static no.nav.pto.veilarbportefolje.database.Table.OPPFOLGINGSBRUKER.FODSELSNR;

public class TestDataClient {

    private final JdbcTemplate jdbcTemplateOracle;
    private final JdbcTemplate jdbcTemplatePostgres;
    private final RegistreringRepositoryV2 registreringRepositoryV2;
    private final OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2;
    private final ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;
    private final OpensearchTestClient opensearchTestClient;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    public TestDataClient(JdbcTemplate jdbcTemplateOracle, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplatePostgres, RegistreringRepositoryV2 registreringRepositoryV2, OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2, ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2, OpensearchTestClient opensearchTestClient, OppfolgingRepositoryV2 oppfolgingRepositoryV2) {
        this.jdbcTemplateOracle = jdbcTemplateOracle;
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.registreringRepositoryV2 = registreringRepositoryV2;
        this.oppfolgingsbrukerRepositoryV2 = oppfolgingsbrukerRepositoryV2;
        this.arbeidslisteRepositoryV2 = arbeidslisteRepositoryV2;
        this.opensearchTestClient = opensearchTestClient;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
    }

    public void endreNavKontorForBruker(AktorId aktoerId, NavKontor navKontor) {
        jdbcTemplatePostgres.update("update oppfolgingsbruker_arena set nav_kontor = ? where aktoerid = ?",
                navKontor.getValue(), aktoerId.get());

        final String fnr = SqlUtils.select(jdbcTemplateOracle, Table.VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> rs.getString(Table.VW_PORTEFOLJE_INFO.FODSELSNR))
                .column(Table.VW_PORTEFOLJE_INFO.FODSELSNR)
                .where(WhereClause.equals(Table.VW_PORTEFOLJE_INFO.AKTOERID, aktoerId.get()))
                .execute();

        SqlUtils.update(jdbcTemplateOracle, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .set(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontor.getValue())
                .whereEquals(FODSELSNR, fnr)
                .execute();
    }

    public void setupBrukerMedArbeidsliste(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        arbeidslisteRepositoryV2.insertArbeidsliste(new ArbeidslisteDTO(fnr)
                .setAktorId(aktoerId)
                .setNavKontorForArbeidsliste(navKontor.getValue())
                .setVeilederId(veilederId)
                .setKategori(Arbeidsliste.Kategori.GUL)
        );

        SqlUtils.insert(jdbcTemplateOracle, Table.ARBEIDSLISTE.TABLE_NAME)
                .value(AKTOERID, aktoerId.get())
                .value(NAV_KONTOR_FOR_ARBEIDSLISTE, navKontor.getValue())
                .execute();

        setupBruker(aktoerId, fnr, navKontor, veilederId, startDato);
        setupBrukerOracle(aktoerId, fnr, navKontor, veilederId, startDato);
        opensearchTestClient.oppdaterArbeidsliste(aktoerId, true);
    }

    public void setupBruker(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        final Fnr fnr = TestDataUtils.randomFnr();
        setupBrukerOracle(aktoerId, fnr, navKontor, veilederId, startDato);
        setupBruker(aktoerId, fnr, navKontor, veilederId, startDato);
    }

    public boolean hentOppfolgingFlaggFraDatabase(AktorId aktoerId) {
        return oppfolgingRepositoryV2.erUnderOppfolging(aktoerId);
    }

    private void setupBruker(AktorId aktoerId, Fnr fnr, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId, startDato);
        oppfolgingRepositoryV2.settVeileder(aktoerId, veilederId);
        registreringRepositoryV2.upsertBrukerRegistrering(new ArbeidssokerRegistrertEvent(aktoerId.get(), null, null, null, null, null));
        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktoerId.get(), fnr.get(), null, null,
                        null, null, navKontor.getValue(), null, null,
                        null, null, null, true, false,
                        false, null, null));

        opensearchTestClient.createUserInOpensearch(aktoerId);
    }


    private void setupBrukerOracle(AktorId aktoerId, Fnr fnr, NavKontor navKontor, VeilederId veilederId, ZonedDateTime startDato) {
        final PersonId personId = TestDataUtils.randomPersonId();
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId, startDato);

        SqlUtils.insert(jdbcTemplateOracle, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.getValue())
                .value(FODSELSNR, fnr.get())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontor.getValue())
                .execute();

        SqlUtils.insert(jdbcTemplateOracle, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktoerId.get())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.getValue())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();

        SqlUtils.insert(jdbcTemplateOracle, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.getValue())
                .value(Table.OPPFOLGING_DATA.STARTDATO, Timestamp.from(startDato.toInstant()))
                .execute();
    }
}
