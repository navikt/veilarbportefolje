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

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier.ENDRET_AKTIVITET;
import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategorier.NY_AKTIVITET;
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

    public void siste_endring_ulike_typer_aktivteter() {
        final AktoerId aktoerId = randomAktoerId();
        elasticTestClient.createUserInElastic(aktoerId);
        String endretTid = "2020-05-28T09:47:42.48+02:00";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(endretTid);

        send_aktvitet_melding(aktoerId,null);
        send_aktvitet_melding(aktoerId,endretTid);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        String endring_tidspunkt = (String) getResponse.getSourceAsMap().get("siste_endring_endret_aktivitet");
        String ny_tidspunkt = (String) getResponse.getSourceAsMap().get("siste_endring_ny_aktivitet");

        assertThat(endring_tidspunkt).isEqualTo(zonedDateTime.toString());
        assertThat(ny_tidspunkt).isNotNull();
        assertThat(!ny_tidspunkt.equals(endring_tidspunkt)).isTrue();
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
        send_aktvitet_melding(aktoerId,endretTid);

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
        filtervalg.setSisteEndringKategori(List.of(ENDRET_AKTIVITET.name()));
        return filtervalg;
    }

    private static Filtervalg getFiltervalgAlleAktivitetEndringer() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.setSisteEndringKategori(List.of(NY_AKTIVITET.name(), ENDRET_AKTIVITET.name()));
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
