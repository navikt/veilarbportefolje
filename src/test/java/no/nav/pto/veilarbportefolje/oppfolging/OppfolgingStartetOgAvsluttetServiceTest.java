package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtak;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO.Hovedmal.BEHOLDE_ARBEID;
import static no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakKafkaDTO.Innsatsgruppe.STANDARD_INNSATS;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
class OppfolgingStartetOgAvsluttetServiceTest extends EndToEndTest {
    private final OppfolgingPeriodeService oppfolgingPeriodeService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private Siste14aVedtakService siste14aVedtakService;

    @Autowired
    private Siste14aVedtakRepository siste14aVedtakRepository;

    @Autowired
    public OppfolgingStartetOgAvsluttetServiceTest(OppfolgingAvsluttetService oppfolgingAvsluttetService, OppfolgingRepositoryV2 oppfolgingRepositoryV2, JdbcTemplate jdbcTemplate) {
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.jdbcTemplate = jdbcTemplate;
        AktorClient aktorClient = mock(AktorClient.class);
        Mockito.when(aktorClient.hentFnr(any())).thenReturn(Fnr.of("-1"));
        OppfolgingStartetService oppfolgingStartetService = new OppfolgingStartetService(oppfolgingRepositoryV2, mock(OpensearchIndexer.class), mock(PdlService.class));
        this.oppfolgingPeriodeService = new OppfolgingPeriodeService(oppfolgingStartetService, oppfolgingAvsluttetService);
    }

    @Test
    void skal_sette_bruker_under_oppfølging_i_databasen() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        SisteOppfolgingsperiodeV1 melding = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse("2020-12-01T00:00:00+02:00"), null);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(melding);

        final BrukerOppdatertInformasjon info = oppfolgingRepositoryV2.hentOppfolgingData(aktoerId).orElseThrow();
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

        SisteOppfolgingsperiodeV1 melding = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse("2020-11-01T00:00:01+02:00"), ZonedDateTime.parse("2020-12-01T00:00:01+02:00"));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(melding);
        List<String> arbeidsliste = jdbcTemplate.queryForList("SELECT aktoerid from arbeidsliste where aktoerid= ?",
                String.class, aktoerId.get());
        List<String> registrering = jdbcTemplate.query("select * from bruker_registrering where aktoerid = ?", (r, i) -> r.getString("aktoerid"), aktoerId.get());

        assertThat(arbeidsliste.isEmpty()).isTrue();
        assertThat(registrering.size()).isEqualTo(0);
        assertThat(testDataClient.hentUnderOppfolgingOgAktivIdent(aktoerId)).isFalse();
        Map<String, Object> source = opensearchTestClient.fetchDocument(aktoerId).getSourceAsMap();
        assertThat(source).isNull();
    }

    @Test
    void skal_slette_siste_14a_vedtak_når_oppfølging_avsluttes() {
        final AktorId aktorId = randomAktorId();

        testDataClient.setupBruker(aktorId, ZonedDateTime.parse("2020-01-01T00:00:01+02:00"));

        siste14aVedtakService.lagreSiste14aVedtak(
                new Siste14aVedtak(aktorId.get(), STANDARD_INNSATS, BEHOLDE_ARBEID, ZonedDateTime.now(), false)
        );

        assertFalse(siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get()))).isEmpty());

        avsluttOppfolging(aktorId, "2020-01-01T00:00:01+02:00", "2020-02-02T00:00:01+02:00");

        assertTrue(siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get()))).isEmpty());
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

    private Optional<BrukerOppdatertInformasjon> startOgAvsluttBruker(String startDato, String sluttDato) {
        final AktorId aktorId = randomAktorId();

        startOppfolging(aktorId, startDato);

        avsluttOppfolging(aktorId, startDato, sluttDato);

        return oppfolgingRepositoryV2.hentOppfolgingData(aktorId);
    }

    private void startOppfolging(AktorId aktorId, String startDato) {
        SisteOppfolgingsperiodeV1 oppfolgingStartetPayload = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktorId.get(), ZonedDateTime.parse(startDato), null);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(oppfolgingStartetPayload);
    }

    private void avsluttOppfolging(AktorId aktorId, String startDato, String sluttDato) {
        SisteOppfolgingsperiodeV1 oppfolgingAvsluttePayload = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktorId.get(), ZonedDateTime.parse(startDato), ZonedDateTime.parse(sluttDato));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(oppfolgingAvsluttePayload);
    }
}
