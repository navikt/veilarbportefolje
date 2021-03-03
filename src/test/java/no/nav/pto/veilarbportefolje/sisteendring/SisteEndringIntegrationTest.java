package no.nav.pto.veilarbportefolje.sisteendring;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.aktiviteter.KafkaAktivitetMelding;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.elasticsearch.action.get.GetResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.*;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringJUnit4ClassRunner.class)
public class SisteEndringIntegrationTest extends EndToEndTest {
    private final MalService malService;
    private final AktivitetService aktivitetService;
    private final BrukerService brukerService;
    private final ElasticService elasticService;
    private final SistLestService sistLestService;
    private final VeilederId veilederId = VeilederId.of("Z123456");

    @Autowired
    public SisteEndringIntegrationTest(MalService malService, ElasticService elasticService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering, SisteEndringService sisteEndringService, SistLestService sistLestService) {
        this.sistLestService = sistLestService;
        brukerService = Mockito.mock(BrukerService.class);
        Mockito.when(brukerService.hentPersonidFraAktoerid(any())).thenReturn(Try.of(TestDataUtils::randomPersonId));
        Mockito.when(brukerService.hentVeilederForBruker(any())).thenReturn(Optional.of(veilederId));

        this.aktivitetService = new AktivitetService(aktivitetDAO, persistentOppdatering,  brukerService, sisteEndringService);
        this.elasticService = elasticService;
        this.malService = malService;
    }

    @Test
    public void siste_endring_mal() {
        final AktorId aktoerId = randomAktorId();
        elasticTestClient.createUserInElastic(aktoerId);
        String endretTid = "2020-05-28T07:47:42.480Z";
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse(endretTid);

        send_mal_melding(aktoerId,endretTidZonedDateTime);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_mal= getValueFromNestedObject(getResponse, "mal");

        assertThat(endring_mal).isNotNull();
        assertThat(endring_mal).isEqualTo(endretTidZonedDateTime.toString());
    }

    @Test
    public void siste_endring_aktivteter() {
        final AktorId aktoerId = randomAktorId();
        elasticTestClient.createUserInElastic(aktoerId);
        String endretTid = "2020-05-28T07:47:42.480Z";
        String endretTid_nyijobb = "2028-05-28T07:47:42.480Z";
        String endretTidSisteEndring = "2020-11-26T10:40:40.000Z";
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse(endretTid);
        ZonedDateTime endretTidZonedDateTime_NY_IJOBB = ZonedDateTime.parse(endretTid_nyijobb);
        ZonedDateTime endretTidNyZonedDateTime = ZonedDateTime.parse(endretTidSisteEndring);

        send_aktvitet_melding(aktoerId, endretTid_nyijobb, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, endretTid, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_fullfort_ijobb = getValueFromNestedObject(getResponse, "fullfort_ijobb");
        String endring_ny_ijobb = getValueFromNestedObject(getResponse, "ny_ijobb");

        assertThat(endring_fullfort_ijobb).isEqualTo(endretTidZonedDateTime.toString());

        assertThat(endring_ny_ijobb).isNotNull();
        assertThat(endring_ny_ijobb).isEqualTo(endretTidZonedDateTime_NY_IJOBB.toString());

        send_aktvitet_melding(aktoerId, endretTidSisteEndring, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        GetResponse getResponse_2 = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse_2.isExists()).isTrue();

        String endring_fullfort_ijobb_2 = getValueFromNestedObject(getResponse_2, "fullfort_ijobb");
        assertThat(endring_fullfort_ijobb_2).isEqualTo(endretTidNyZonedDateTime.toString());
    }

    @Test
    public void siste_endring_filter_test() {
        final String testEnhet = "0000";
        final AktorId aktoerId = randomAktorId();
        String endretTid = "2019-05-28T09:47:42.48+02:00";
        String endretTid_ny = "2020-05-28T09:47:42.48+02:00";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(endretTid);
        ZonedDateTime zonedDateTime_NY_IJOBB = ZonedDateTime.parse(endretTid_ny);

        populateElastic(aktoerId.toString());

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        send_aktvitet_melding(aktoerId, endretTid_ny, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, endretTid, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_IJOBB),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        var responseBrukere = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(FULLFORT_IJOBB),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertThat(responseBrukere.getBrukere().get(0).getSisteEndringTidspunkt()).isEqualTo(zonedDateTime.toLocalDateTime());

        var responseBrukere_2 = elasticService.hentBrukere(
                testEnhet,
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
    public void siste_endring_ulest_filter_test() {
        final String testEnhet = "0000";
        final AktorId aktoerId = randomAktorId();
        String endretTid = "2019-05-28T09:47:42.48+02:00";
        String endretTid_ny = "2020-05-28T09:47:42.48+02:00";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(endretTid);
        ZonedDateTime zonedDateTime_NY_IJOBB = ZonedDateTime.parse(endretTid_ny);

        String lestAvVeilederTid = "2012-07-28T09:47:42.48+02:00";

        populateElastic(aktoerId.toString());

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });
        send_sett_aktiitetsplan(aktoerId,lestAvVeilederTid);

        send_aktvitet_melding(aktoerId, endretTid_ny, KafkaAktivitetMelding.EndringsType.OPPRETTET,
                KafkaAktivitetMelding.AktivitetStatus.PLANLAGT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId, endretTid, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalg(NY_IJOBB),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        var responseBrukere = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalg(NY_IJOBB, true),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertThat(responseBrukere.getBrukere().get(0).getSisteEndringTidspunkt()).isEqualTo(zonedDateTime.toLocalDateTime());

        var responseBrukere_2 = elasticService.hentBrukere(
                testEnhet,
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
    public void siste_endring_sortering_test() {
        final String testEnhet = "0000";
        final AktorId aktoerId_1 = randomAktorId();
        final AktorId aktoerId_2 = randomAktorId();
        final AktorId aktoerId_3 = randomAktorId();

        String endret_Tid_IJOBB_bruker_1_i_2024 = "2024-05-28T09:47:42.480Z";
        String endret_Tid_IJOBB_bruker_2_i_2025 = "2025-05-28T09:47:42.480Z";

        String endret_Tid_EGEN_bruker_1_i_2021 = "2021-05-28T07:47:42.480Z";
        String endret_Tid_EGEN_bruker_2_i_2020 = "2020-05-28T06:47:42.480Z";
        String endret_Tid_EGEN_bruker_3_i_2019 = "2019-05-28T00:47:42.480Z";

        populateElastic(aktoerId_1.get(), aktoerId_2.get(), aktoerId_3.get());
        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        send_aktvitet_melding(aktoerId_1,endret_Tid_IJOBB_bruker_1_i_2024, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);
        send_aktvitet_melding(aktoerId_2, endret_Tid_IJOBB_bruker_2_i_2025, KafkaAktivitetMelding.EndringsType.FLYTTET,
                KafkaAktivitetMelding.AktivitetStatus.FULLFORT,
                KafkaAktivitetMelding.AktivitetTypeData.IJOBB);

        send_aktvitet_melding(aktoerId_1,endret_Tid_EGEN_bruker_1_i_2021, KafkaAktivitetMelding.EndringsType.FLYTTET,
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
                    testEnhet,
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
                    testEnhet,
                    empty(),
                    "ascending",
                    "ikke_satt",
                    getFiltervalg(FULLFORT_EGEN),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        var responseSortertFULLFORT_IJOBB = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(FULLFORT_IJOBB),
                null,
                null);

        assertThat(responseSortertFULLFORT_IJOBB.getAntall()).isEqualTo(2);
        assertThat(responseSortertFULLFORT_IJOBB.getBrukere().get(0).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_IJOBB_bruker_2_i_2025).getYear());
        assertThat(responseSortertFULLFORT_IJOBB.getBrukere().get(1).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_IJOBB_bruker_1_i_2024).getYear());

        var responseSortertFULLFORT_EGEN = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "ascending",
                "siste_endring_tidspunkt",
                getFiltervalg(FULLFORT_EGEN),
                null,
                null);

        assertThat(responseSortertFULLFORT_EGEN.getAntall()).isEqualTo(3);
        assertThat(responseSortertFULLFORT_EGEN.getBrukere().get(0).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_EGEN_bruker_3_i_2019).getYear());
        assertThat(responseSortertFULLFORT_EGEN.getBrukere().get(1).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_EGEN_bruker_2_i_2020).getYear());
        assertThat(responseSortertFULLFORT_EGEN.getBrukere().get(2).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_EGEN_bruker_1_i_2021).getYear());

        var responseSortertFULLFORT_MIX = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(FULLFORT_IJOBB, FULLFORT_EGEN),
                null,
                null);

        assertThat(responseSortertFULLFORT_MIX.getAntall()).isEqualTo(3);
        assertThat(responseSortertFULLFORT_MIX.getBrukere().get(0).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_IJOBB_bruker_2_i_2025).getYear());
        assertThat(responseSortertFULLFORT_MIX.getBrukere().get(1).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_IJOBB_bruker_1_i_2024).getYear());
        assertThat(responseSortertFULLFORT_MIX.getBrukere().get(2).getSisteEndringTidspunkt().getYear()).isEqualTo(ZonedDateTime.parse(endret_Tid_EGEN_bruker_3_i_2019).getYear());

        var responseSortertTomRes1 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(NY_IJOBB),
                null,
                null);
        assertThat(responseSortertTomRes1.getAntall()).isEqualTo(0);

        var responseSortertTomRes2 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "descending",
                "siste_endring_tidspunkt",
                getFiltervalg(NY_IJOBB,NY_EGEN),
                null,
                null);
        assertThat(responseSortertTomRes2.getAntall()).isEqualTo(0);
    }

    private void send_aktvitet_melding(AktorId aktoerId, String endretDato, KafkaAktivitetMelding.EndringsType endringsType,
                                       KafkaAktivitetMelding.AktivitetStatus status, KafkaAktivitetMelding.AktivitetTypeData typeData) {
        String endret = endretDato == null ? "" : "\"endretDato\":\""+endretDato+"\",";
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\""+aktoerId.get()+"\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                    endret +
                "\"aktivitetType\":\""+typeData+"\"," +
                "\"aktivitetStatus\":\""+status+"\"," +
                "\"endringsType\":\""+endringsType+"\"," +
                "\"lagtInnAv\":\"BRUKER\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";
        aktivitetService.behandleKafkaMelding(aktivitetKafkaMelding);
    }


    private void send_sett_aktiitetsplan(AktorId aktoerId, String settDato) {
        String sistLestKafkaMelding = "{" +
                "\"aktorId\":\""+aktoerId.get()+"\"," +
                "\"harLestTidspunkt\":\""+settDato+"\"," +
                "\"veilederId\":\""+veilederId.getValue()+"\"" +
                "}";
        sistLestService.behandleKafkaMelding(sistLestKafkaMelding);
    }

    private void send_mal_melding(AktorId aktoerId, ZonedDateTime endretDato) {
        String endret = endretDato == null ? "" : "\"endretTidspunk\":\""+endretDato+"\"";
        String kafkamelding = "{" +
                "\"aktorId\":\""+aktoerId.get()+"\"," +
                "\"lagtInnAv\":\"BRUKER\"," +
                "\"veilederIdent\":\"Z12345\"," +
                endret +
                "}";
        malService.behandleKafkaMelding(kafkamelding);
    }

    private static Filtervalg getFiltervalgUleste() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setUlesteEndringer("ULESTE_ENDRINGER");
        return filtervalg;
    }

    private static Filtervalg getFiltervalg(SisteEndringsKategori kategori) {
        return getFiltervalg(kategori, false);
    }

    private static Filtervalg getFiltervalg(SisteEndringsKategori kategori, boolean uleste) {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(kategori.name()));
        if(uleste){
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

    private String getValueFromNestedObject(GetResponse respons, String field){
        assertThat(respons).isNotNull();
        Object nestedObject = respons.getSourceAsMap().get("siste_endringer");
        if(nestedObject instanceof Map) {
            return  ((Map<String, Map<String, String>>) nestedObject).get(field).get("tidspunkt");
        }
        return null;
    }

    private void populateElastic(String... aktoerIder) {
        List<OppfolgingsBruker> brukere =  new ArrayList<>();
        for (String aktoerId: aktoerIder) {
            brukere.add( new OppfolgingsBruker()
                    .setAktoer_id(aktoerId)
                    .setOppfolging(true)
                    .setEnhet_id("0000")
                    .setVeileder_id(veilederId.getValue())
                    );
        }

        brukere.forEach(bruker -> elasticTestClient.createUserInElastic(bruker));
    }
}
