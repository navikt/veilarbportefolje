package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
class OppfolgingStartetOgAvsluttetServiceTest extends EndToEndTest {
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final OppfolgingStartetService oppfolgingStartetService;
    private final OppfolgingPeriodeService oppfolgingPeriodeService;
    private final OppfolgingRepository oppfolgingRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OppfolgingStartetOgAvsluttetServiceTest(OppfolgingAvsluttetService oppfolgingAvsluttetService, OppfolgingRepository oppfolgingRepository, OppfolgingRepositoryV2 oppfolgingRepositoryV2, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate, OpensearchIndexer opensearchIndexer, OppfolgingPeriodeService oppfolgingPeriodeService) {
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.jdbcTemplate = jdbcTemplate;
        AktorClient aktorClient = mock(AktorClient.class);
        BrukerRepository brukerRepository = mock(BrukerRepository.class);
        Mockito.when(aktorClient.hentFnr(any())).thenReturn(Fnr.of("-1"));
        when(brukerRepository.retrievePersonidFromFnr(Fnr.of("-1"))).thenReturn(Optional.of(PersonId.of("0000")));
        this.oppfolgingStartetService = new OppfolgingStartetService(oppfolgingRepository, oppfolgingRepositoryV2, opensearchIndexer);
        this.oppfolgingPeriodeService = new OppfolgingPeriodeService(this.oppfolgingStartetService, this.oppfolgingAvsluttetService);
    }

    @Test
    void skal_sette_bruker_under_oppfølging_i_databasen() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        SisteOppfolgingsperiodeV1 melding = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse("2020-12-01T00:00:00+02:00"), null);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(melding);

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

        SisteOppfolgingsperiodeV1 melding = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse("2020-11-01T00:00:01+02:00"), ZonedDateTime.parse("2020-12-01T00:00:01+02:00"));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(melding);
        String arbeidsliste = SqlUtils
                .select(jdbcTemplate, Table.ARBEIDSLISTE.TABLE_NAME, rs -> rs.getString(Table.ARBEIDSLISTE.AKTOERID))
                .column(Table.ARBEIDSLISTE.AKTOERID)
                .where(WhereClause.equals(Table.ARBEIDSLISTE.AKTOERID, aktoerId.get()))
                .execute();

        assertThat(arbeidsliste).isNull();

        List<String> registrering = jdbcTemplate.query("select * from bruker_registrering where aktoerid = ?", (r, i) -> r.getString("aktoerid"), aktoerId.get());

        assertThat(registrering.size()).isEqualTo(0);
        assertThat(testDataClient.hentOppfolgingFlaggFraDatabase(aktoerId)).isFalse();
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

    private Optional<BrukerOppdatertInformasjon> startOgAvsluttBruker(String startDato, String sluttDato) {
        final AktorId aktoerId = randomAktorId();

        SisteOppfolgingsperiodeV1 oppfolgingStartetPayload = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse(startDato), null);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(oppfolgingStartetPayload);

        SisteOppfolgingsperiodeV1 oppfolgingAvsluttePayload = new SisteOppfolgingsperiodeV1(UUID.randomUUID(), aktoerId.get(), ZonedDateTime.parse(startDato), ZonedDateTime.parse(sluttDato));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(oppfolgingAvsluttePayload);

        return oppfolgingRepository.hentOppfolgingData(aktoerId);
    }
}
