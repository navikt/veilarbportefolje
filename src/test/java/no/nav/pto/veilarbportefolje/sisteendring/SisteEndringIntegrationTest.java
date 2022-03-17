package no.nav.pto.veilarbportefolje.sisteendring;

import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.mal.MalEndringKafkaDTO;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sistelest.SistLestKafkaMelding;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.action.get.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.FULLFORT_EGEN;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.FULLFORT_IJOBB;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.MAL;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.NY_IJOBB;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class SisteEndringIntegrationTest extends EndToEndTest {
    private final MalService malService;
    private final JdbcTemplate jdbcTemplate;
    private final AktivitetService aktivitetService;
    private final OpensearchService opensearchService;
    private final SistLestService sistLestService;
    private final OppfolgingRepository oppfolgingRepositoryMock;
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");
    private final Fnr fodselsnummer1 = Fnr.ofValidFnr("10108000000"); //TESTFAMILIE
    private final Fnr fodselsnummer2 = Fnr.ofValidFnr("11108000000"); //TESTFAMILIE
    private final Fnr fodselsnummer3 = Fnr.ofValidFnr("12108000000"); //TESTFAMILIE
    private Long aktivitetVersion = 1L;

    @Autowired
    public SisteEndringIntegrationTest(MalService malService, JdbcTemplate jdbcTemplate, OpensearchService opensearchService, SisteEndringService sisteEndringService, AktiviteterRepositoryV2 aktiviteterRepositoryV2, OpensearchIndexer opensearchIndexer) {
        this.jdbcTemplate = jdbcTemplate;
        BrukerService brukerService = mock(BrukerService.class);
        Mockito.when(brukerService.hentPersonidFraAktoerid(any())).thenReturn(Try.of(TestDataUtils::randomPersonId));
        Mockito.when(brukerService.hentVeilederForBruker(any())).thenReturn(Optional.of(veilederId));
        unleashService = Mockito.mock(UnleashService.class);
        this.oppfolgingRepositoryMock = mock(OppfolgingRepository.class);
        this.aktivitetService = new AktivitetService(aktiviteterRepositoryV2, brukerService, sisteEndringService, opensearchIndexer);
        this.sistLestService = new SistLestService(brukerService, sisteEndringService);
        this.opensearchService = opensearchService;
        this.malService = malService;
    }

    @BeforeEach
    public void resetMock() {
        jdbcTemplate.execute("truncate table aktoerid_to_personid");
        jdbcTemplate.execute("truncate table siste_endring");
        jdbcTemplate.execute("truncate table oppfolging_data");
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table aktiviteter");
        Mockito.when(oppfolgingRepositoryMock.erUnderoppfolging(any())).thenReturn(true);
    }

    @Test
    public void sisteendring_populering_mal() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, fodselsnummer1, testEnhet.get());
        populateOpensearch(testEnhet, veilederId, aktoerId.get());
        String endretTid = "2020-05-28T07:47:42.480Z";
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse(endretTid);

        send_mal_melding(aktoerId, endretTidZonedDateTime);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_mal = getValueFromNestedObject(getResponse, MAL);

        assertThat(endring_mal).isNotNull();
        assertThat(endring_mal).isEqualTo(endretTidZonedDateTime.toString());
    }

    //@Test
    public void sisteendring_populering_aktiviteter() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, fodselsnummer1, testEnhet.get());
        populateOpensearch(testEnhet,veilederId, aktoerId.get());
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse("2020-05-28T07:47:42.480Z");
        ZonedDateTime endretTidZonedDateTime_NY_IJOBB = ZonedDateTime.parse("2028-05-28T07:47:42.480Z");
        ZonedDateTime endretTidNyZonedDateTime = ZonedDateTime.parse("2020-11-26T10:40:40.000Z");

        send_aktvitet_melding(aktoerId, endretTidZonedDateTime_NY_IJOBB, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, endretTidZonedDateTime, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_fullfort_ijobb = getValueFromNestedObject(getResponse, FULLFORT_IJOBB);
        String endring_ny_ijobb = getValueFromNestedObject(getResponse, NY_IJOBB);

        assertThat(endring_fullfort_ijobb).isEqualTo(endretTidZonedDateTime.toString());

        assertThat(endring_ny_ijobb).isNotNull();
        assertThat(endring_ny_ijobb).isEqualTo(endretTidZonedDateTime_NY_IJOBB.toString());

        send_aktvitet_melding(aktoerId, endretTidNyZonedDateTime, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        GetResponse getResponse_2 = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse_2.isExists()).isTrue();

        String endring_fullfort_ijobb_2 = getValueFromNestedObject(getResponse_2, FULLFORT_IJOBB);
        assertThat(endring_fullfort_ijobb_2).isEqualTo(endretTidNyZonedDateTime.toString());
    }

    //@Test
    public void sisteendring_filtrering() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, fodselsnummer1, testEnhet.get());
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2019-05-28T09:47:42.48+02:00");
        ZonedDateTime zonedDateTime_NY_IJOBB = ZonedDateTime.parse("2020-05-28T09:47:42.48+02:00");

        send_aktvitet_melding(aktoerId, zonedDateTime, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, zonedDateTime_NY_IJOBB, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB),
                    null,
                    null);
            System.out.println(brukereMedAntall.getAntall());
            return brukereMedAntall.getAntall() == 1;
        });
        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {

            var responseBrukere = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB),
                    null,
                    null);

            assertThat(responseBrukere.getAntall()).isEqualTo(1);
            assertThat(responseBrukere.getBrukere().get(0).getSisteEndringTidspunkt()).isEqualTo(zonedDateTime.toLocalDateTime());

        });
}


    @Test
    public void sisteendring_ulestfilter_skalIkkeKrasjeVedNull() {
        final AktorId aktoerId = randomAktorId();
        populateOpensearch(testEnhet, veilederId, aktoerId.toString());
        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        var responseBrukere = opensearchService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(FULLFORT_IJOBB, true),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(0);
    }

    //@Test
    public void sisteendring_ulestfilter() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.setupBruker(aktoerId, fodselsnummer1, testEnhet.get());
        ZonedDateTime endretTid_FULLFORT_IJOBB = ZonedDateTime.parse("2019-05-28T09:47:42.48+02:00");
        ZonedDateTime endretTid_NY_IJOBB = ZonedDateTime.parse("2020-05-28T09:47:42.48+02:00");

        ZonedDateTime lestAvVeilederTid = ZonedDateTime.parse("2019-07-28T09:47:42.48+02:00");

        populateOpensearch(testEnhet, veilederId, aktoerId.toString());
        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        send_aktvitet_melding(aktoerId, endretTid_NY_IJOBB,
                KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, endretTid_FULLFORT_IJOBB,
                KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(NY_IJOBB),
                    null,
                    null);
            return brukereMedAntall.getAntall() == 1;
        });

        send_sett_aktivitetsplan(aktoerId, lestAvVeilederTid);

        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB, true),
                    null,
                    null);
            return brukereMedAntall.getAntall() == 0;
        });

        var responseBrukere1 = opensearchService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(NY_IJOBB, true),
                null,
                null);

        assertThat(responseBrukere1.getAntall()).isEqualTo(1);

        var responseBrukere2 = opensearchService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(FULLFORT_IJOBB, true),
                null,
                null);

        assertThat(responseBrukere2.getAntall()).isEqualTo(0);
    }

    @Test
    public void sisteendring_sortering() {
        final AktorId aktoerId_1 = randomAktorId();
        final AktorId aktoerId_2 = randomAktorId();
        final AktorId aktoerId_3 = randomAktorId();
        testDataClient.setupBruker(aktoerId_1, fodselsnummer1, testEnhet.get());
        testDataClient.setupBruker(aktoerId_2, fodselsnummer2, testEnhet.get());
        testDataClient.setupBruker(aktoerId_3, fodselsnummer3, testEnhet.get());

        ZonedDateTime endret_Tid_IJOBB_bruker_1_i_2024 = ZonedDateTime.parse("2024-05-28T09:47:42.480Z");
        ZonedDateTime endret_Tid_IJOBB_bruker_2_i_2025 = ZonedDateTime.parse("2025-05-28T09:47:42.480Z");

        ZonedDateTime endret_Tid_EGEN_bruker_1_i_2021 = ZonedDateTime.parse("2021-05-28T07:47:42.480Z");
        ZonedDateTime endret_Tid_EGEN_bruker_2_i_2020 = ZonedDateTime.parse("2020-05-28T06:47:42.480Z");
        ZonedDateTime endret_Tid_EGEN_bruker_3_i_2019 = ZonedDateTime.parse("2019-05-28T00:47:42.480Z");

        populateOpensearch(testEnhet, veilederId, aktoerId_1.get(), aktoerId_2.get(), aktoerId_3.get());
        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        send_aktvitet_melding(aktoerId_1, endret_Tid_IJOBB_bruker_1_i_2024, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId_2, endret_Tid_IJOBB_bruker_2_i_2025, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        send_aktvitet_melding(aktoerId_1, endret_Tid_EGEN_bruker_1_i_2021, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.EGEN);
        send_aktvitet_melding(aktoerId_2, endret_Tid_EGEN_bruker_2_i_2020, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.EGEN);
        send_aktvitet_melding(aktoerId_3, endret_Tid_EGEN_bruker_3_i_2019, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.EGEN);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId_1);
        assertThat(getResponse.isExists()).isTrue();

        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "ascending",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 2;
        });

        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "ascending",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_EGEN),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        var responseSortertFULLFORT_IJOBB = opensearchService.hentBrukere(
                testEnhet.get(),
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(FULLFORT_IJOBB),
                null,
                null);

        assertThat(responseSortertFULLFORT_IJOBB.getAntall()).isEqualTo(2);
        assertThat(responseSortertFULLFORT_IJOBB.getBrukere().get(0).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_IJOBB_bruker_2_i_2025.getYear());
        assertThat(responseSortertFULLFORT_IJOBB.getBrukere().get(1).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_IJOBB_bruker_1_i_2024.getYear());

        var responseSortertFULLFORT_EGEN = opensearchService.hentBrukere(
                testEnhet.get(),
                empty(),
                "ascending",
                "siste_endring_tidspunkt",
                getFiltervalg(FULLFORT_EGEN),
                null,
                null);

        assertThat(responseSortertFULLFORT_EGEN.getAntall()).isEqualTo(3);
        assertThat(responseSortertFULLFORT_EGEN.getBrukere().get(0).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_EGEN_bruker_3_i_2019.getYear());
        assertThat(responseSortertFULLFORT_EGEN.getBrukere().get(1).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_EGEN_bruker_2_i_2020.getYear());
        assertThat(responseSortertFULLFORT_EGEN.getBrukere().get(2).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_EGEN_bruker_1_i_2021.getYear());

        var responseSortertTomRes1 = opensearchService.hentBrukere(
                testEnhet.get(),
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(NY_IJOBB),
                null,
                null);
        assertThat(responseSortertTomRes1.getAntall()).isEqualTo(0);
    }

    @Test
    public void sisteendring_filterPaFlereEndringerSkalKasteError() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> opensearchService.hentBrukere(
                        testEnhet.get(),
                        empty(),
                        "descending",
                        "siste_endring_tidspunkt",
                        getFiltervalg(FULLFORT_IJOBB, FULLFORT_EGEN),
                        null,
                        null));
        assertThat(exception).isNotNull();
    }

    private void send_aktvitet_melding(AktorId aktoerId, ZonedDateTime endretDato, KafkaAktivitetMelding.EndringsType endringsType,
                                       KafkaAktivitetMelding.AktivitetStatus status, KafkaAktivitetMelding.AktivitetTypeData typeData) {
        KafkaAktivitetMelding melding = new KafkaAktivitetMelding().setAktivitetId("144136")
                .setAktorId(aktoerId.get()).setFraDato(ZonedDateTime.now().minusDays(5)).setEndretDato(endretDato)
                .setAktivitetType(typeData).setAktivitetStatus(status).setEndringsType(endringsType).setLagtInnAv(KafkaAktivitetMelding.InnsenderData.BRUKER)
                .setAvtalt(true).setHistorisk(false).setVersion(aktivitetVersion++);
        aktivitetService.behandleKafkaMeldingLogikk(melding);
    }


    private void send_sett_aktivitetsplan(AktorId aktoerId, ZonedDateTime settDato) {
        SistLestKafkaMelding melding = new SistLestKafkaMelding().setAktorId(aktoerId).setHarLestTidspunkt(settDato).setVeilederId(veilederId);
        sistLestService.behandleKafkaMeldingLogikk(melding);
    }

    private void send_mal_melding(AktorId aktoerId, ZonedDateTime endretDato) {
        MalEndringKafkaDTO kafkamelding = new MalEndringKafkaDTO()
                .setAktorId(aktoerId.get())
                .setLagtInnAv(MalEndringKafkaDTO.InnsenderData.BRUKER)
                .setVeilederIdent("Z12345")
                .setEndretTidspunk(endretDato);
        malService.behandleKafkaMeldingLogikk(kafkamelding);
    }

    private static Filtervalg getFiltervalg(SisteEndringsKategori kategori) {
        return getFiltervalg(kategori, false);
    }

    private static Filtervalg getFiltervalg(SisteEndringsKategori kategori, boolean uleste) {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(kategori.name()));
        if (uleste) {
            filtervalg.setUlesteEndringer("ULESTE_ENDRINGER");
        }
        return filtervalg;
    }

    private static Filtervalg getFiltervalg(SisteEndringsKategori kategori_1, SisteEndringsKategori kategori_2) {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(kategori_1.name(), kategori_2.name()));
        return filtervalg;
    }

    private String getValueFromNestedObject(GetResponse respons, SisteEndringsKategori field) {
        assertThat(respons).isNotNull();
        Object nestedObject = respons.getSourceAsMap().get("siste_endringer");
        if (nestedObject instanceof Map) {
            return ((Map<String, Map<String, String>>) nestedObject).get(field.name()).get("tidspunkt");
        }
        return null;
    }
}
