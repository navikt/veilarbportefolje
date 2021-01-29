package no.nav.pto.veilarbportefolje.util;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.value.*;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.pto.veilarbportefolje.database.Table.ARBEIDSLISTE.AKTOERID;
import static no.nav.pto.veilarbportefolje.database.Table.ARBEIDSLISTE.NAV_KONTOR_FOR_ARBEIDSLISTE;
import static no.nav.pto.veilarbportefolje.database.Table.OPPFOLGINGSBRUKER.FODSELSNR;

public class TestDataClient {

    private final JdbcTemplate jdbcTemplate;
    private final ElasticTestClient elasticTestClient;

    @Autowired
    public TestDataClient(JdbcTemplate jdbcTemplate, ElasticTestClient elasticTestClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.elasticTestClient = elasticTestClient;
    }

    public void endreNavKontorForBruker(AktorId aktoerId, NavKontor navKontor) {
        final String fnr = SqlUtils.select(jdbcTemplate, Table.VW_PORTEFOLJE_INFO.TABLE_NAME, rs -> rs.getString(Table.VW_PORTEFOLJE_INFO.FODSELSNR))
                .column(Table.VW_PORTEFOLJE_INFO.FODSELSNR)
                .where(WhereClause.equals(Table.VW_PORTEFOLJE_INFO.AKTOERID, aktoerId.get()))
                .execute();

        SqlUtils.update(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .set(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontor.getValue())
                .whereEquals(FODSELSNR, fnr)
                .execute();
    }

    public void setupBrukerMedArbeidsliste(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId) {
        SqlUtils.insert(jdbcTemplate, Table.ARBEIDSLISTE.TABLE_NAME)
                .value(AKTOERID, aktoerId.get())
                .value(NAV_KONTOR_FOR_ARBEIDSLISTE, navKontor.getValue())
                .execute();

        setupBruker(aktoerId, navKontor, veilederId);

        elasticTestClient.oppdaterArbeidsliste(aktoerId, true);
    }

    public void setupBruker(AktorId aktoerId, NavKontor navKontor, VeilederId veilederId) {
        final PersonId personId = TestDataUtils.randomPersonId();
        final Fnr fnr = TestDataUtils.randomFnr();

        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.getValue())
                .value(FODSELSNR, fnr.get())
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontor.getValue())
                .execute();

        SqlUtils.insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktoerId.get())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.getValue())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, 1)
                .execute();

        SqlUtils.insert(jdbcTemplate, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.VEILEDERIDENT, veilederId.getValue())
                .execute();

        SqlUtils.insert(jdbcTemplate, Table.BRUKER_REGISTRERING.TABLE_NAME)
                .value(Table.BRUKER_REGISTRERING.AKTOERID, aktoerId.get())
                .execute();

        elasticTestClient.createUserInElastic(aktoerId);
    }

    public String hentOppfolgingFlaggFraDatabase(AktorId aktoerId) {
        return SqlUtils.select(jdbcTemplate, Table.OPPFOLGING_DATA.TABLE_NAME, rs -> rs.getString(Table.OPPFOLGING_DATA.OPPFOLGING))
                .column(Table.OPPFOLGING_DATA.OPPFOLGING)
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get()))
                .execute();
    }
}
