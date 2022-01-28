package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

class OppfolgingStartetOgAvsluttetServiceTest extends EndToEndTest {

    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final OppfolgingStartetService oppfolgingStartetService;
    private final OppfolgingRepository oppfolgingRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OppfolgingStartetOgAvsluttetServiceTest(OppfolgingAvsluttetService oppfolgingAvsluttetService, OppfolgingRepository oppfolgingRepository, JdbcTemplate jdbcTemplate) {
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.jdbcTemplate = jdbcTemplate;
        AktorClient aktorClient = mock(AktorClient.class);
        Mockito.when(aktorClient.hentFnr(any())).thenReturn(Fnr.of("-1"));
        this.oppfolgingStartetService = new OppfolgingStartetService(oppfolgingRepository, mock(OppfolgingRepositoryV2.class), mock(OpensearchIndexer.class), mock(BrukerRepository.class), aktorClient);
    }

    @Test
    void skal_sette_bruker_under_oppfølging_i_databasen() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        OppfolgingStartetDTO melding = new OppfolgingStartetDTO(aktoerId, ZonedDateTime.parse("2020-12-01T00:00:00+02:00"));

        oppfolgingStartetService.behandleKafkaMeldingLogikk(melding);

        final BrukerOppdatertInformasjon info = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();
        assertThat(info.getOppfolging()).isTrue();
        assertThat(info.getNyForVeileder()).isFalse();
    }

    @Test
    void skal_slette_arbeidsliste_registrering_og_avslutte_oppfølging() {
        final AktorId aktoerId = randomAktorId();

        testDataClient.setupBrukerMedArbeidsliste(
                aktoerId,
                randomNavKontor(),
                randomVeilederId(),
                ZonedDateTime.parse("2020-12-01T00:00:00+02:00")
        );

        OppfolgingAvsluttetDTO melding = new OppfolgingAvsluttetDTO(aktoerId, ZonedDateTime.parse("2020-12-01T00:00:01+02:00"));

        oppfolgingAvsluttetService.behandleKafkaMeldingLogikk(melding);

        String arbeidsliste = SqlUtils
                .select(jdbcTemplate, Table.ARBEIDSLISTE.TABLE_NAME, rs -> rs.getString(Table.ARBEIDSLISTE.AKTOERID))
                .column(Table.ARBEIDSLISTE.AKTOERID)
                .where(WhereClause.equals(Table.ARBEIDSLISTE.AKTOERID, aktoerId.get()))
                .execute();

        assertThat(arbeidsliste).isNull();

        String registrering = SqlUtils
                .select(jdbcTemplate, Table.BRUKER_REGISTRERING.TABLE_NAME, rs -> rs.getString(Table.BRUKER_REGISTRERING.AKTOERID))
                .column(Table.BRUKER_REGISTRERING.AKTOERID)
                .where(WhereClause.equals(Table.BRUKER_REGISTRERING.AKTOERID, aktoerId.get()))
                .execute();

        assertThat(registrering).isNull();

        assertThat(testDataClient.hentOppfolgingFlaggFraDatabase(aktoerId)).isNull();

        Map<String, Object> source = opensearchTestClient.fetchDocument(aktoerId).getSourceAsMap();

        assertThat(source).isNull();
    }

    @Test
    void skal_ikke_avslutte_bruker_som_har_startdato_senere_enn_sluttdato() {
        final Optional<BrukerOppdatertInformasjon> bruker = startOgAvsluttBruker("2020-01-01T00:00:01+02:00", "2020-01-01T00:00:00+02:00");
        assertThat(bruker.orElseThrow().getOppfolging()).isTrue();
    }

    @Test
    void skal_avslutte_bruker_som_har_en_tidligere_startdato_enn_sluttdato() {
        final Optional<BrukerOppdatertInformasjon> bruker = startOgAvsluttBruker("2020-01-01T00:00:00+02:00", "2020-01-01T00:00:01+02:00");
        assertThat(bruker).isNotPresent();
    }

    @Test
    void skal_ikke_avslutte_bruker_som_ikke_finnes() {
        final AktorId aktoerId = randomAktorId();

        OppfolgingAvsluttetDTO oppfolgingAvsluttePayload = new OppfolgingAvsluttetDTO(aktoerId, ZonedDateTime.parse("2020-01-01T00:00:00+02:00"));

        oppfolgingAvsluttetService.behandleKafkaMeldingLogikk(oppfolgingAvsluttePayload);
    }

    private Optional<BrukerOppdatertInformasjon> startOgAvsluttBruker(String startDato, String sluttDato) {
        final AktorId aktoerId = randomAktorId();

        final OppfolgingStartetDTO oppfolgingStartetPayload = new OppfolgingStartetDTO(aktoerId, ZonedDateTime.parse(startDato));

        oppfolgingStartetService.behandleKafkaMeldingLogikk(oppfolgingStartetPayload);

        OppfolgingAvsluttetDTO oppfolgingAvsluttePayload = new OppfolgingAvsluttetDTO(aktoerId, ZonedDateTime.parse(sluttDato));

        oppfolgingAvsluttetService.behandleKafkaMeldingLogikk(oppfolgingAvsluttePayload);

        return oppfolgingRepository.hentOppfolgingData(aktoerId);
    }
}
