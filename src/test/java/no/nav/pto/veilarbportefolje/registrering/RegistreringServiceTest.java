package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.opensearch.action.get.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RegistreringServiceTest extends EndToEndTest {

    private final RegistreringService registreringService;
    private final OpensearchService opensearchService;
    private final OpensearchIndexer indexer;

    @Autowired
    public RegistreringServiceTest(RegistreringService registreringService, OpensearchService opensearchService, OpensearchIndexer indexer) {
        this.registreringService = registreringService;
        this.opensearchService = opensearchService;
        this.indexer = indexer;
    }


    @Test
    void utdanning_full_integration() {
        final AktorId aktoerId = randomAktorId();
        populateOpensearch(EnhetId.of("0000"), VeilederId.of(null), aktoerId.get());
        opensearchTestClient.createUserInOpensearch(aktoerId);

        ArbeidssokerRegistrertEvent kafkaMessage = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(aktoerId.toString())
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.now().format(ISO_ZONED_DATE_TIME))
                .build();

        registreringService.behandleKafkaMeldingLogikk(kafkaMessage);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);

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
        populateOpensearch(testEnhet);

        // Må vente til dokumentet blir søkbart i Opensearch
        pollOpensearchUntil(() -> {
            final BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere(
                    testEnhet,
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg(),
                    null,
                    null);

            return brukereMedAntall.getAntall() == 3;
        });

        var responseBrukere2 = opensearchService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgBestatt(),
                null,
                null);

        // Trenger ikke vente som ovenfor da dokumentene mest sannsynlig nå er søkbare
        assertThat(responseBrukere2.getAntall()).isEqualTo(1);

        var responseBrukere3 = opensearchService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgGodkjent(),
                null,
                null);

        assertThat(responseBrukere3.getAntall()).isEqualTo(2);

        var responseBrukere4 = opensearchService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgUtdanning(),
                null,
                null);

        assertThat(responseBrukere4.getAntall()).isEqualTo(2);

        var responseBrukere5 = opensearchService.hentBrukere(
                testEnhet,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgMix(),
                null,
                null);

        assertThat(responseBrukere5.getAntall()).isEqualTo(1);
    }

    private static Filtervalg getFiltervalgBestatt() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>()); // TODO: Denne må være der, er det en bug?
        filtervalg.utdanningBestatt.add(UtdanningBestattSvar.JA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgGodkjent() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanningGodkjent.add(UtdanningGodkjentSvar.JA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgUtdanning() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add(UtdanningSvar.GRUNNSKOLE);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgMix() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add(UtdanningSvar.GRUNNSKOLE);
        filtervalg.utdanningGodkjent.add(UtdanningGodkjentSvar.JA);
        filtervalg.utdanningBestatt.add(UtdanningBestattSvar.NEI);
        return filtervalg;
    }

    private void populateOpensearch(String enhet) {
        final AktorId aktoerId1 = randomAktorId();
        final AktorId aktoerId2 = randomAktorId();
        final AktorId aktoerId3 = randomAktorId();

        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId1.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("NEI"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId2.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("JA")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId3.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE")
        );

        brukere.forEach(bruker -> {
                    populateOpensearch(EnhetId.of(enhet), VeilederId.of(null), bruker.getAktoer_id());
                    indexer.skrivTilIndeks(indexName.getValue(), bruker);
                }
        );

    }
}
