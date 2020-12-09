package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.elasticsearch.action.get.GetResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktoerId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RegistreringServiceTest extends EndToEndTest {

    private final RegistreringService registreringService;
    private final ElasticService elasticService;

    @Autowired
    public RegistreringServiceTest(RegistreringService registreringService, ElasticService elasticService) {
        this.registreringService = registreringService;
        this.elasticService = elasticService;
    }

    @Test
    void utdanning_full_integration() {
        final AktoerId aktoerId = randomAktoerId();

        elasticTestClient.createUserInElastic(aktoerId);

        ArbeidssokerRegistrertEvent kafkaMessage = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(aktoerId.toString())
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.now(ZoneId.of("Europe/Oslo")).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringService.behandleKafkaMelding(kafkaMessage);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);

        assertThat(getResponse.isExists()).isTrue();

        String utdanning = (String) getResponse.getSourceAsMap().get("utdanning");
        String situasjon = (String) getResponse.getSourceAsMap().get("brukers_situasjon");
        String utdanningBestatt = (String) getResponse.getSourceAsMap().get("utdanning_bestatt");
        String utdanningGodkjent = (String) getResponse.getSourceAsMap().get("utdanning_godkjent");

        assertThat(utdanning).isEqualTo(UtdanningSvar.GRUNNSKOLE.toString());
        assertThat(situasjon).isEqualTo("Permittert");
        assertThat(utdanningBestatt).isEqualTo(UtdanningBestattSvar.INGEN_SVAR.toString());
        assertThat(utdanningGodkjent).isEqualTo(UtdanningGodkjentSvar.JA.toString());
    }

    @Test
    void utdanning_filter_test() {
        final String testEnhet = "0000";

        populateElastic(testEnhet);

        // Må vente til dokumentet blir søkbart i elastic
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

        var responseBrukere2 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgBestatt(),
                null,
                null);

        // Trenger ikke vente som ovenfor da dokumentene mest sannsynlig nå er søkbare
        assertThat(responseBrukere2.getAntall()).isEqualTo(1);

        var responseBrukere3 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgGodkjent(),
                null,
                null);

        assertThat(responseBrukere3.getAntall()).isEqualTo(2);

        var responseBrukere4 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgUtdanning(),
                null,
                null);

        assertThat(responseBrukere4.getAntall()).isEqualTo(2);

        var responseBrukere5 = elasticService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgMix(),
                null,
                null);

        assertThat(responseBrukere5.getAntall()).isEqualTo(1);
    }

    private static Filtervalg getFiltervalgBestatt(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>()); // TODO: Denne må være der, er det en bug?
        filtervalg.utdanningBestatt.add("JA");
        return filtervalg;
    }

    private static Filtervalg getFiltervalgGodkjent(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanningGodkjent.add("JA");
        return filtervalg;
    }

    private static Filtervalg getFiltervalgUtdanning(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add("GRUNNSKOLE");
        return filtervalg;
    }

    private static Filtervalg getFiltervalgMix(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add("GRUNNSKOLE");
        filtervalg.utdanningGodkjent.add("JA");
        filtervalg.utdanningBestatt.add("NEI");
        return filtervalg;
    }

    private void populateElastic(String enhet) {
        final AktoerId aktoerId1 = TestDataUtils.randomAktoerId();
        final AktoerId aktoerId2 = TestDataUtils.randomAktoerId();
        final AktoerId aktoerId3 = TestDataUtils.randomAktoerId();

        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId1.toString())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("NEI"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId2.toString())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("JA")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId3.toString())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE")
        );

        brukere.forEach(bruker -> elasticTestClient.createUserInElastic(bruker));
    }
}
