package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.*;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktoerId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SisteEndringIntegrationTest extends EndToEndTest {
    private final AktivitetService aktivitetService;
    private final ElasticService elasticService;

    @Autowired
    public SisteEndringIntegrationTest(AktivitetService aktivitetService, ElasticService elasticService) {
        this.aktivitetService = aktivitetService;
        this.elasticService = elasticService;
    }

    @Test
    public void siste_endring_aktivteter() {
        final AktoerId aktoerId = randomAktoerId();
        elasticTestClient.createUserInElastic(aktoerId);
        String endretTid = "2020-05-28T07:47:42.480Z";
        String endretTidSisteEndring = "2020-11-26T10:40:40.000Z";
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse(endretTid);
        ZonedDateTime endretTidNyZonedDateTime = ZonedDateTime.parse(endretTidSisteEndring);

        send_aktvitet_melding(aktoerId,null,"IJOBB","FULLFORT");
        send_aktvitet_melding(aktoerId, endretTid,"IJOBB","FULLFORT");

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_fullfort_ijobb = getValueFromNestedObject(getResponse, "fullfort_ijobb");
        String endring_ny_ijobb = getValueFromNestedObject(getResponse, "ny_ijobb");

        assertThat(endring_fullfort_ijobb).isEqualTo(endretTidZonedDateTime.toString());

        assertThat(endring_ny_ijobb).isNotNull();
        assertThat(!endring_ny_ijobb.equals(endring_fullfort_ijobb)).isTrue();

        send_aktvitet_melding(aktoerId, endretTidSisteEndring,"IJOBB","FULLFORT");
        GetResponse getResponse_2 = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse_2.isExists()).isTrue();

        String endring_fullfort_ijobb_2 = getValueFromNestedObject(getResponse_2, "fullfort_ijobb");
        assertThat(endring_fullfort_ijobb_2).isEqualTo(endretTidNyZonedDateTime.toString());
    }

    @Test
    void siste_endring_filter_test() {
        final String testEnhet = "0000";
        final AktoerId aktoerId = randomAktoerId();
        String endretTid = "2020-05-28T09:47:42.48+02:00";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(endretTid);

        populateElastic(testEnhet, aktoerId.toString());

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

        send_aktvitet_melding(aktoerId,null,"IJOBB","FULLFORT");
        send_aktvitet_melding(aktoerId, endretTid,"IJOBB","FULLFORT");

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
        assertThat(!respons_tidspunkt.equals(zonedDateTime.toLocalDateTime())).isTrue();

    }

    @Test
    void siste_endring_sortering_test() {
        final String testEnhet = "0000";
        final AktoerId aktoerId_1 = randomAktoerId();
        final AktoerId aktoerId_2 = randomAktoerId();
        final AktoerId aktoerId_3 = randomAktoerId();

        String endret_Tid_IJOBB_bruker_1_i_2024 = "2024-05-28T09:47:42.480Z";
        String endret_Tid_IJOBB_bruker_2_i_2025 = "2025-05-28T09:47:42.480Z";

        String endret_Tid_EGEN_bruker_1_i_2021 = "2021-05-28T07:47:42.480Z";
        String endret_Tid_EGEN_bruker_2_i_2020 = "2020-05-28T06:47:42.480Z";
        String endret_Tid_EGEN_bruker_3_i_2019 = "2019-05-28T00:47:42.480Z";

        populateElastic(testEnhet, aktoerId_1.getValue(), aktoerId_2.getValue(), aktoerId_3.getValue());
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

        send_aktvitet_melding(aktoerId_1,endret_Tid_IJOBB_bruker_1_i_2024,"IJOBB","FULLFORT");
        send_aktvitet_melding(aktoerId_2, endret_Tid_IJOBB_bruker_2_i_2025,"IJOBB","FULLFORT");

        send_aktvitet_melding(aktoerId_1,endret_Tid_EGEN_bruker_1_i_2021,"EGEN","FULLFORT");
        send_aktvitet_melding(aktoerId_2, endret_Tid_EGEN_bruker_2_i_2020,"EGEN","FULLFORT");
        send_aktvitet_melding(aktoerId_3, endret_Tid_EGEN_bruker_3_i_2019,"EGEN","FULLFORT");

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


    private void send_aktvitet_melding(AktoerId aktoerId, String endretDato, String type, String status) {
        String endret = endretDato == null ? "" : "\"endretDato\":\""+endretDato+"\",";
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\""+aktoerId.getValue()+"\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                    endret +
                "\"aktivitetType\":\""+type+"\"," +
                "\"aktivitetStatus\":\""+status+"\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";
        aktivitetService.behandleKafkaMelding(aktivitetKafkaMelding);
    }

    private static Filtervalg getFiltervalg(SisteEndringsKategori kategori) {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(kategori.name()));
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
            return  ((Map<String, String>) nestedObject).get(field);
        }
        return null;
    }

    private void populateElastic(String enhet, String... aktoerIder) {
        List<OppfolgingsBruker> brukere =  new ArrayList<>();
        for (String aktoerId: aktoerIder) {
            brukere.add( new OppfolgingsBruker()
                    .setAktoer_id(aktoerId)
                    .setOppfolging(true)
                    .setEnhet_id(enhet));
        }

        brukere.forEach(bruker -> elasticTestClient.createUserInElastic(bruker));
    }
}
