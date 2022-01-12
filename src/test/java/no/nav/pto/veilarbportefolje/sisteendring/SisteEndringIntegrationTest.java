package no.nav.pto.veilarbportefolje.sisteendring;

import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatusRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.AktiviteterRepositoryV2;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.mal.MalEndringKafkaDTO;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sistelest.SistLestKafkaMelding;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.FULLFORT_EGEN;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.FULLFORT_IJOBB;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.MAL;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.NY_EGEN;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.NY_IJOBB;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

public class SisteEndringIntegrationTest extends EndToEndTest {
    private final MalService malService;
    private final AktivitetService aktivitetService;
    private final BrukerService brukerService;
    private final ElasticService elasticService;
    private final SistLestService sistLestService;
    private final OppfolgingRepository oppfolgingRepositoryMock;
    private final VeilederId veilederId = VeilederId.of("Z123456");
    private final EnhetId testEnhet = EnhetId.of("0000");

    @Autowired
    public SisteEndringIntegrationTest(MalService malService, ElasticService elasticService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering, SisteEndringService sisteEndringService, AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2, AktiviteterRepositoryV2 aktiviteterRepositoryV2, BrukerDataService brukerDataService, ElasticIndexer elasticIndexer) {
        brukerService = mock(BrukerService.class);
        Mockito.when(brukerService.hentPersonidFraAktoerid(any())).thenReturn(Try.of(TestDataUtils::randomPersonId));
        Mockito.when(brukerService.hentVeilederForBruker(any())).thenReturn(Optional.of(veilederId));
        unleashService = Mockito.mock(UnleashService.class);
        this.oppfolgingRepositoryMock = mock(OppfolgingRepository.class);
        this.aktivitetService = new AktivitetService(aktivitetDAO, aktiviteterRepositoryV2, aktivitetStatusRepositoryV2, persistentOppdatering, brukerService, brukerDataService, sisteEndringService, mock(UnleashService.class), elasticIndexer);
        this.sistLestService = new SistLestService(brukerService, sisteEndringService);
        this.elasticService = elasticService;
        this.malService = malService;
    }

    @BeforeEach
    public void resetMock(){
        Mockito.when(oppfolgingRepositoryMock.erUnderoppfolging(any())).thenReturn(true);
    }

    @Test
    public void siste_endring_mal() {
        final AktorId aktoerId = randomAktorId();
        elasticTestClient.createUserInElastic(aktoerId);
        String endretTid = "2020-05-28T07:47:42.480Z";
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse(endretTid);

        send_mal_melding(aktoerId, endretTidZonedDateTime);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_mal = getValueFromNestedObject(getResponse, MAL);

        assertThat(endring_mal).isNotNull();
        assertThat(endring_mal).isEqualTo(endretTidZonedDateTime.toString());
    }

    @Test
    public void siste_endring_aktivteter() {
        final AktorId aktoerId = randomAktorId();
        elasticTestClient.createUserInElastic(aktoerId);
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse("2020-05-28T07:47:42.480Z");
        ZonedDateTime endretTidZonedDateTime_NY_IJOBB = ZonedDateTime.parse("2028-05-28T07:47:42.480Z");
        ZonedDateTime endretTidNyZonedDateTime = ZonedDateTime.parse("2020-11-26T10:40:40.000Z");

        send_aktvitet_melding(aktoerId, endretTidZonedDateTime_NY_IJOBB, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, endretTidZonedDateTime, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_fullfort_ijobb = getValueFromNestedObject(getResponse, FULLFORT_IJOBB);
        String endring_ny_ijobb = getValueFromNestedObject(getResponse, NY_IJOBB);

        assertThat(endring_fullfort_ijobb).isEqualTo(endretTidZonedDateTime.toString());

        assertThat(endring_ny_ijobb).isNotNull();
        assertThat(endring_ny_ijobb).isEqualTo(endretTidZonedDateTime_NY_IJOBB.toString());

        send_aktvitet_melding(aktoerId, endretTidNyZonedDateTime, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        GetResponse getResponse_2 = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse_2.isExists()).isTrue();

        String endring_fullfort_ijobb_2 = getValueFromNestedObject(getResponse_2, FULLFORT_IJOBB);
        assertThat(endring_fullfort_ijobb_2).isEqualTo(endretTidNyZonedDateTime.toString());
    }

    @Test
    public void siste_endring_filter_test() {
        final AktorId aktoerId = randomAktorId();
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2019-05-28T09:47:42.48+02:00");
        ZonedDateTime zonedDateTime_NY_IJOBB = ZonedDateTime.parse( "2020-05-28T09:47:42.48+02:00");

        populateElastic(testEnhet, veilederId, aktoerId.toString());

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        send_aktvitet_melding(aktoerId, zonedDateTime_NY_IJOBB, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, zonedDateTime, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        var responseBrukere = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(FULLFORT_IJOBB),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertThat(responseBrukere.getBrukere().get(0).getSisteEndringTidspunkt()).isEqualTo(zonedDateTime.toLocalDateTime());

        var responseBrukere_2 = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(NY_IJOBB, FULLFORT_IJOBB),
                null,
                null);
        assertThat(responseBrukere_2.getAntall()).isEqualTo(1);
        var respons_tidspunkt = responseBrukere_2.getBrukere().get(0).getSisteEndringTidspunkt();

        assertThat(respons_tidspunkt).isNotNull();
        assertThat(respons_tidspunkt).isEqualTo(zonedDateTime_NY_IJOBB.toLocalDateTime());

    }


    @Test
    public void siste_endring_ulest_skal_ikke_krasje_med_null_verdier() {
        final AktorId aktoerId = randomAktorId();
        populateElastic(testEnhet, veilederId, aktoerId.toString());
        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        var responseBrukere = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(FULLFORT_IJOBB, true),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(0);
    }

    @Test
    public void siste_endring_ulest_filter_test() {
        final AktorId aktoerId = randomAktorId();
        ZonedDateTime endretTid_FULLFORT_IJOBB = ZonedDateTime.parse("2019-05-28T09:47:42.48+02:00");
        ZonedDateTime endretTid_NY_IJOBB = ZonedDateTime.parse("2020-05-28T09:47:42.48+02:00");

        ZonedDateTime lestAvVeilederTid = ZonedDateTime.parse("2019-07-28T09:47:42.48+02:00");

        populateElastic(testEnhet, veilederId, aktoerId.toString());
        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
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

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
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

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB, true),
                    null,
                    null);
            return brukereMedAntall.getAntall() == 0;
        });

        var responseBrukere1 = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(NY_IJOBB, true),
                null,
                null);

        assertThat(responseBrukere1.getAntall()).isEqualTo(1);

        var responseBrukere2 = elasticService.hentBrukere(
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
    public void siste_endring_sortering_test() {
        final AktorId aktoerId_1 = randomAktorId();
        final AktorId aktoerId_2 = randomAktorId();
        final AktorId aktoerId_3 = randomAktorId();

        ZonedDateTime endret_Tid_IJOBB_bruker_1_i_2024 = ZonedDateTime.parse("2024-05-28T09:47:42.480Z");
        ZonedDateTime endret_Tid_IJOBB_bruker_2_i_2025 = ZonedDateTime.parse("2025-05-28T09:47:42.480Z");

        ZonedDateTime endret_Tid_EGEN_bruker_1_i_2021 = ZonedDateTime.parse("2021-05-28T07:47:42.480Z");
        ZonedDateTime endret_Tid_EGEN_bruker_2_i_2020 = ZonedDateTime.parse("2020-05-28T06:47:42.480Z");
        ZonedDateTime endret_Tid_EGEN_bruker_3_i_2019 = ZonedDateTime.parse("2019-05-28T00:47:42.480Z");

        populateElastic(testEnhet, veilederId, aktoerId_1.get(), aktoerId_2.get(), aktoerId_3.get());
        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
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

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId_1);
        assertThat(getResponse.isExists()).isTrue();

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "ascending",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 2;
        });

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet.get(),
                    empty(),
                    "ascending",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_EGEN),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        var responseSortertFULLFORT_IJOBB = elasticService.hentBrukere(
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

        var responseSortertFULLFORT_EGEN = elasticService.hentBrukere(
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

        var responseSortertFULLFORT_MIX = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(FULLFORT_IJOBB, FULLFORT_EGEN),
                null,
                null);

        assertThat(responseSortertFULLFORT_MIX.getAntall()).isEqualTo(3);
        assertThat(responseSortertFULLFORT_MIX.getBrukere().get(0).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_IJOBB_bruker_2_i_2025.getYear());
        assertThat(responseSortertFULLFORT_MIX.getBrukere().get(1).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_IJOBB_bruker_1_i_2024.getYear());
        assertThat(responseSortertFULLFORT_MIX.getBrukere().get(2).getSisteEndringTidspunkt().getYear()).isEqualTo(endret_Tid_EGEN_bruker_3_i_2019.getYear());

        var responseSortertTomRes1 = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(NY_IJOBB),
                null,
                null);
        assertThat(responseSortertTomRes1.getAntall()).isEqualTo(0);

        var responseSortertTomRes2 = elasticService.hentBrukere(
                testEnhet.get(),
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(NY_IJOBB, NY_EGEN),
                null,
                null);
        assertThat(responseSortertTomRes2.getAntall()).isEqualTo(0);
    }

    private void send_aktvitet_melding(AktorId aktoerId, ZonedDateTime endretDato, KafkaAktivitetMelding.EndringsType endringsType,
                                       KafkaAktivitetMelding.AktivitetStatus status, KafkaAktivitetMelding.AktivitetTypeData typeData) {
        KafkaAktivitetMelding melding = new KafkaAktivitetMelding().setAktivitetId("144136")
                .setAktorId(aktoerId.get()).setFraDato(ZonedDateTime.now().minusDays(5)).setEndretDato(endretDato)
                .setAktivitetType(typeData).setAktivitetStatus(status).setEndringsType(endringsType).setLagtInnAv(KafkaAktivitetMelding.InnsenderData.BRUKER)
                .setAvtalt(true).setHistorisk(false).setVersion(49179898L);
        aktivitetService.behandleKafkaMeldingLogikk(melding);
    }


    private void send_sett_aktivitetsplan(AktorId aktoerId, ZonedDateTime settDato) {
        SistLestKafkaMelding melding =  new SistLestKafkaMelding().setAktorId(aktoerId).setHarLestTidspunkt(settDato).setVeilederId(veilederId);
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
