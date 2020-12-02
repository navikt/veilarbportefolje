package no.nav.pto.veilarbportefolje.arbeidsliste;

import lombok.Value;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.sbl.sql.SqlUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Timestamp;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArbeidslisteServiceTest {


    private static ArbeidslisteService arbeidslisteService;
    private static JdbcTemplate jdbcTemplate;
    private static AktorregisterClient aktorregisterClientMock;

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);

        ArbeidslisteRepository arbeidslisteRepository = new ArbeidslisteRepository(jdbcTemplate, namedParameterJdbcTemplate);
        BrukerRepository brukerRepository = new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate);

        aktorregisterClientMock = mock(AktorregisterClient.class);

        BrukerService brukerService = new BrukerService(brukerRepository, aktorregisterClientMock);

        arbeidslisteService = new ArbeidslisteService(aktorregisterClientMock, arbeidslisteRepository, brukerService, mock(ElasticServiceV2.class), mock(MetricsClient.class));
    }

    @Before
    @After
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.ARBEIDSLISTE.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
    }

    @Test
    public void skal_inserte_fnr_i_arbeidslisten() {
        String aktoerId = "00000000000";
        String excpectedNavKontor = "00000000000";
        FnrOgNavKontor fnrOgNavKontor = setUpInitialState(aktoerId, excpectedNavKontor);

        String actualFnr = SqlUtils
                .select(jdbcTemplate, Table.ARBEIDSLISTE.TABLE_NAME, rs -> rs.getString(Table.ARBEIDSLISTE.FNR))
                .column(Table.ARBEIDSLISTE.FNR)
                .execute();

        assertThat(actualFnr).isEqualTo(fnrOgNavKontor.getFnr());

        String actualNavKontor = arbeidslisteService.hentNavKontorForArbeidsliste(AktoerId.of(aktoerId)).orElseThrow();

        assertThat(actualNavKontor).isEqualTo(excpectedNavKontor);
    }

    @Test
    public void skal_inserte_nav_kontor_i_arbeidsliste() {
        String aktoerId = "00000000000";
        String navKontor = "00000000000";
        FnrOgNavKontor fnrOgNavKontor = setUpInitialState(aktoerId, navKontor);
    }

    private static FnrOgNavKontor setUpInitialState(String aktoerId, String navKontor) {
        String fnr = "00000000000";
        String personId = "00000000000";

        when(aktorregisterClientMock.hentAktorId(anyString())).thenReturn(aktoerId);

        SqlUtils
                .insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr)
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId)
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontor)
                .execute();

        SqlUtils
                .insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktoerId)
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId)
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, true)
                .execute();

        ArbeidslisteDTO dto = new ArbeidslisteDTO(Fnr.of(fnr))
                .setNavKontorForArbeidsliste("0000")
                .setAktoerId(AktoerId.of("0"))
                .setVeilederId(VeilederId.of("0"))
                .setFrist(Timestamp.from(now()))
                .setKategori(Arbeidsliste.Kategori.BLA)
                .setOverskrift("foo");

        arbeidslisteService.createArbeidsliste(dto);
        return new FnrOgNavKontor(fnr, navKontor);
    }

    @Value
    private static class FnrOgNavKontor {
        String fnr;
        String navKontor;
    }

}