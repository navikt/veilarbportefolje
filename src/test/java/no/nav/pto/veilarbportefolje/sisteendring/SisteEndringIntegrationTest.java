package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier.*;
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
        String endretTid = "2020-05-28T09:47:42.48+02:00";
        String endretTidSisteEndring = "2020-11-28T09:47:42.48+02:00";
        ZonedDateTime endretTidZonedDateTime = ZonedDateTime.parse(endretTid);
        ZonedDateTime endretTidNyZonedDateTime = ZonedDateTime.parse(endretTidSisteEndring);

        send_aktvitet_melding(aktoerId,null);
        send_aktvitet_melding(aktoerId, endretTid);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_fullfort_ijobb = getValueFromNestedObject(getResponse, "fullfort_ijobb");
        String endring_ny_ijobb = getValueFromNestedObject(getResponse, "ny_ijobb");

        assertThat(endring_fullfort_ijobb).isEqualTo(endretTidZonedDateTime.toString());

        assertThat(endring_ny_ijobb).isNotNull();
        assertThat(!endring_ny_ijobb.equals(endring_fullfort_ijobb)).isTrue();

        send_aktvitet_melding(aktoerId, endretTidSisteEndring);
        GetResponse getResponse_2 = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse_2.isExists()).isTrue();

        String endring_fullfort_ijobb_2 = getValueFromNestedObject(getResponse_2, "fullfort_ijobb");
        assertThat(endring_fullfort_ijobb_2).isEqualTo(endretTidNyZonedDateTime.toString());
    }

    private String getValueFromNestedObject(GetResponse respons, String field){
        assertThat(respons).isNotNull();
        Object nestedObject = respons.getSourceAsMap().get("siste_endringer");
        if(nestedObject instanceof Map) {
            return  ((Map<String, String>) nestedObject).get(field);
        }
        return null;
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

        send_aktvitet_melding(aktoerId,null);
        send_aktvitet_melding(aktoerId, endretTid);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        pollElasticUntil(() -> {
            final BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    getFiltervalgEndretAktivitet(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 1;
        });

        var responseBrukere = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgEndretAktivitet(),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertThat(responseBrukere.getBrukere().get(0).getSisteEndringTidspunkt()).isEqualTo(zonedDateTime.toLocalDateTime());

        var responseBrukere_2 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgAlleAktivitetEndringer(),
                null,
                null);
        assertThat(responseBrukere_2.getAntall()).isEqualTo(1);
        var respons_tidspunkt = responseBrukere_2.getBrukere().get(0).getSisteEndringTidspunkt();

        assertThat(respons_tidspunkt).isNotNull();
        assertThat(!respons_tidspunkt.equals(zonedDateTime.toLocalDateTime())).isTrue();

    }


    private void send_aktvitet_melding(AktoerId aktoerId, String endretDato) {
        String endret = endretDato == null ? "" : "\"endretDato\":\""+endretDato+"\",";
        String aktivitetKafkaMelding = "{" +
                "\"aktivitetId\":\"144136\"," +
                "\"aktorId\":\""+aktoerId.getValue()+"\"," +
                "\"fraDato\":\"2020-07-09T12:00:00+02:00\"," +
                "\"tilDato\":null," +
                    endret +
                "\"aktivitetType\":\"IJOBB\"," +
                "\"aktivitetStatus\":\"FULLFORT\"," +
                "\"avtalt\":true," +
                "\"historisk\":false" +
                "}";
        aktivitetService.behandleKafkaMelding(aktivitetKafkaMelding);
    }

    private static Filtervalg getFiltervalgEndretAktivitet() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(FULLFORT_IJOBB.name()));
        return filtervalg;
    }

    private static Filtervalg getFiltervalgAlleAktivitetEndringer() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(NY_IJOBB.name(), FULLFORT_IJOBB.name()));
        return filtervalg;
    }

    private void populateElastic(String enhet, String aktoerId) {
        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId)
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
        );

        brukere.forEach(bruker -> elasticTestClient.createUserInElastic(bruker));
    }
}
