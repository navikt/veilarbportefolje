package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepository;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.LeaderElectionClientMock;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.sbl.sql.SqlUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Timestamp;
import java.util.List;

import static java.time.Instant.now;
import static no.nav.pto.veilarbportefolje.database.Table.ARBEIDSLISTE.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingFeedHandlerIntegrationTest {

    private static OppfolgingFeedHandler oppfolgingFeedHandler;
    private static ArbeidslisteService arbeidslisteService;
    private static JdbcTemplate jdbcTemplate;
    private static AktorregisterClient aktorregisterClientMock;

    @BeforeClass
    public static void beforeClass() {
        ElasticIndexer elasticIndexerMock = mock(ElasticIndexer.class);

        aktorregisterClientMock = mock(AktorregisterClient.class);

        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);

        BrukerRepository brukerRepository = new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate);
        ArbeidslisteRepository arbeidslisteRepository = new ArbeidslisteRepository(jdbcTemplate, namedParameterJdbcTemplate);

        BrukerService brukerService = new BrukerService(brukerRepository, aktorregisterClientMock);

        arbeidslisteService = new ArbeidslisteService(
                aktorregisterClientMock,
                arbeidslisteRepository,
                brukerService,
                elasticIndexerMock,
                mock(MetricsClient.class)
        );

        OppfolgingRepository oppfolgingRepository = new OppfolgingRepository(jdbcTemplate);

        UnleashService unleashMock = mock(UnleashService.class);
        when(unleashMock.isEnabled(anyString())).thenReturn(true);

        oppfolgingFeedHandler = new OppfolgingFeedHandler(
                arbeidslisteService,
                new BrukerService(brukerRepository, aktorregisterClientMock),
                elasticIndexerMock,
                oppfolgingRepository,
                new TestTransactor(),
                new LeaderElectionClientMock()
        );
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.ARBEIDSLISTE.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
    }

    @Test
    public void skal_hente_nav_kontor_fra_db_link_om_vi_ikke_har_mappet_inn_aktoer_id() {
        String aktoerId = "11111111111";
        String navKontor = "0000";

        setUpInitialState(aktoerId, navKontor, navKontor, true);
        sendMeldingPaaFeed(aktoerId);

        Arbeidsliste arbeidsliste = arbeidslisteService.getArbeidsliste(AktoerId.of(aktoerId))
                .getOrElseThrow(() -> new RuntimeException());

        assertThat(arbeidsliste).isNotNull();
    }

    @Test
    public void skal_ikke_slette_arbeidsliste_om_bruker_har_samme_nav_kontor_i_arena_som_vi_har_lagret_paa_arbeidslisten() {
        String aktoerId = "11111111111";
        String navKontor = "0000";

        setUpInitialState(aktoerId, navKontor, navKontor, false);
        sendMeldingPaaFeed(aktoerId);

        Arbeidsliste arbeidsliste = arbeidslisteService.getArbeidsliste(AktoerId.of(aktoerId))
                .getOrElseThrow(() -> new RuntimeException());

        assertThat(arbeidsliste).isNotNull();
    }


    @Test
    public void skal_slette_arbeidsliste_om_bruker_ikke_har_samme_nav_kontor_i_arena_som_vi_har_lagret_paa_arbeidslisten() {
        String aktoerId = "11111111111";
        String navKontorArena = "0000";
        String navKontorArbeidsliste = "1111";

        setUpInitialState(aktoerId, navKontorArena, navKontorArbeidsliste, false);
        sendMeldingPaaFeed(aktoerId);

        Arbeidsliste arbeidsliste = arbeidslisteService.getArbeidsliste(AktoerId.of(aktoerId))
                .getOrElseThrow(() -> new RuntimeException());

        assertThat(arbeidsliste).isNull();
    }

    private static void sendMeldingPaaFeed(String aktoerId) {
        List<BrukerOppdatertInformasjon> feedData =
                List.of(new BrukerOppdatertInformasjon()
                        .setOppfolging(true)
                        .setAktoerid(aktoerId)
                );

        oppfolgingFeedHandler.call("", feedData);
    }

    private static void setUpInitialState(String aktoerId, String navKontorArena, String navKontorArbeidsliste, boolean harIkkeMappetAktoerId) {
        String fnr = "00000000000";
        String personId = "0";

        if (harIkkeMappetAktoerId) {
            when(aktorregisterClientMock.hentFnr(anyString())).thenReturn(fnr);
        } else {
            when(aktorregisterClientMock.hentAktorId(anyString())).thenReturn(aktoerId);
        }

        SqlUtils
                .insert(jdbcTemplate, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr)
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId)
                .value(Table.OPPFOLGINGSBRUKER.NAV_KONTOR, navKontorArena)
                .execute();

        if (!harIkkeMappetAktoerId) {
            SqlUtils
                    .insert(jdbcTemplate, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                    .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktoerId)
                    .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId)
                    .value(Table.AKTOERID_TO_PERSONID.GJELDENE, true)
                    .execute();
        }

        ArbeidslisteDTO dto = new ArbeidslisteDTO(Fnr.of(fnr))
                .setNavKontorForArbeidsliste(navKontorArbeidsliste)
                .setAktoerId(AktoerId.of(aktoerId))
                .setVeilederId(VeilederId.of("0"))
                .setFrist(Timestamp.from(now()))
                .setKategori(Arbeidsliste.Kategori.BLA)
                .setOverskrift("foo");

        SqlUtils
                .insert(jdbcTemplate, TABLE_NAME)
                .value(AKTOERID, dto.getAktoerId().toString())
                .value(FNR, dto.getFnr().toString())
                .value(SIST_ENDRET_AV_VEILEDERIDENT, dto.getVeilederId().toString())
                .value(ENDRINGSTIDSPUNKT, Timestamp.from(now()))
                .value(OVERSKRIFT, dto.getOverskrift())
                .value(KOMMENTAR, dto.getKommentar())
                .value(FRIST, dto.getFrist())
                .value(KATEGORI, dto.getKategori().name())
                .value(NAV_KONTOR_FOR_ARBEIDSLISTE, dto.getNavKontorForArbeidsliste())
                .execute();
    }
}
