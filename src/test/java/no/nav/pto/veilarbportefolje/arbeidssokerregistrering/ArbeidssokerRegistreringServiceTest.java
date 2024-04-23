package no.nav.pto.veilarbportefolje.arbeidssokerregistrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v1.registrering.ArbeidssokerRegistreringService;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.filtervalg.DinSituasjonSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningBestattSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningGodkjentSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningSvar;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
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
import java.util.concurrent.TimeUnit;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ArbeidssokerRegistreringServiceTest extends EndToEndTest {
    private final ArbeidssokerRegistreringService arbeidssokerRegistreringService;
    private final OpensearchService opensearchService;
    private final OpensearchIndexer indexer;

    @Autowired
    public ArbeidssokerRegistreringServiceTest(ArbeidssokerRegistreringService arbeidssokerRegistreringService, OpensearchService opensearchService, OpensearchIndexer indexer) {
        this.arbeidssokerRegistreringService = arbeidssokerRegistreringService;
        this.opensearchService = opensearchService;
        this.indexer = indexer;
    }

    @Test
    void utdanning_full_integration() {
        final AktorId aktoerId = randomAktorId();
        testDataClient.lagreBrukerUnderOppfolging(aktoerId, ZonedDateTime.now());

        ArbeidssokerRegistrertEvent kafkaMessage = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(aktoerId.toString())
                .setBrukersSituasjon("Permittert")
                .setUtdanning(no.nav.arbeid.soker.registrering.UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(no.nav.arbeid.soker.registrering.UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.now().format(ISO_ZONED_DATE_TIME))
                .build();

        arbeidssokerRegistreringService.behandleKafkaMeldingLogikk(kafkaMessage);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);

        assertThat(getResponse.isExists()).isTrue();

        String utdanning = (String) getResponse.getSourceAsMap().get("utdanning");
        String situasjon = (String) getResponse.getSourceAsMap().get("brukers_situasjon");
        String utdanningBestatt = (String) getResponse.getSourceAsMap().get("utdanning_bestatt");
        String utdanningGodkjent = (String) getResponse.getSourceAsMap().get("utdanning_godkjent");

        assertThat(utdanning).isEqualTo(no.nav.arbeid.soker.registrering.UtdanningSvar.GRUNNSKOLE.toString());
        assertThat(situasjon).isEqualTo("Permittert");
        assertThat(utdanningBestatt).isEqualTo(UtdanningBestattSvar.INGEN_SVAR.toString());
        assertThat(utdanningGodkjent).isEqualTo(UtdanningGodkjentSvar.JA.toString());
    }

    @Test
    void utdanning_filter_test() {
        final String testEnhet = "0000";
        populateOpensearch(testEnhet);

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere2 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgBestatt(),
                            null,
                            null);

                    assertThat(responseBrukere2.getAntall()).isEqualTo(1);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere3 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgGodkjent(),
                            null,
                            null);

                    assertThat(responseBrukere3.getAntall()).isEqualTo(4);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere4 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgUtdanning(),
                            null,
                            null);

                    assertThat(responseBrukere4.getAntall()).isEqualTo(2);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
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
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere6 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgIngenUtdanningBestattData(),
                            null,
                            null);

                    assertThat(responseBrukere6.getAntall()).isEqualTo(3);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere7 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgIngenSituasjonsData(),
                            null,
                            null);

                    assertThat(responseBrukere7.getAntall()).isEqualTo(4);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere7 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgIngenUtdanningGodkjentData(),
                            null,
                            null);

                    assertThat(responseBrukere7.getAntall()).isEqualTo(3);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere8 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgIngenUtdanningData(),
                            null,
                            null);

                    assertThat(responseBrukere8.getAntall()).isEqualTo(5);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere9 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgSituasjonsDataMix(),
                            null,
                            null);

                    assertThat(responseBrukere9.getAntall()).isEqualTo(6);
                }
        );

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
                    var responseBrukere10 = opensearchService.hentBrukere(
                            testEnhet,
                            empty(),
                            "asc",
                            "ikke_satt",
                            getFiltervalgUtdanningMix(),
                            null,
                            null);

                    assertThat(responseBrukere10.getAntall()).isEqualTo(7);
                }
        );


    }

    private static Filtervalg getFiltervalgBestatt() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
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

    private static Filtervalg getFiltervalgIngenUtdanningData() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add(UtdanningSvar.INGEN_DATA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgIngenUtdanningGodkjentData() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanningGodkjent.add(UtdanningGodkjentSvar.INGEN_DATA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgIngenUtdanningBestattData() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanningBestatt.add(UtdanningBestattSvar.INGEN_DATA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgIngenSituasjonsData() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.registreringstype.add(DinSituasjonSvar.INGEN_DATA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgSituasjonsDataMix() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.registreringstype.add(DinSituasjonSvar.MISTET_JOBBEN);
        filtervalg.registreringstype.add(DinSituasjonSvar.INGEN_DATA);
        return filtervalg;
    }

    private static Filtervalg getFiltervalgUtdanningMix() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add(UtdanningSvar.GRUNNSKOLE);
        filtervalg.utdanning.add(UtdanningSvar.INGEN_UTDANNING);
        filtervalg.utdanning.add(UtdanningSvar.INGEN_DATA);
        return filtervalg;
    }

    private void populateOpensearch(String enhet) {
        final AktorId aktoerId1 = randomAktorId();
        final AktorId aktoerId2 = randomAktorId();
        final AktorId aktoerId3 = randomAktorId();
        final AktorId aktoerId4 = randomAktorId();
        final AktorId aktoerId5 = randomAktorId();
        final AktorId aktoerId6 = randomAktorId();
        final AktorId aktoerId7 = randomAktorId();
        final AktorId aktoerId8 = randomAktorId();

        List<OppfolgingsBruker> brukere = List.of(
                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId1.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("NEI")
                        .setBrukers_situasjon("MISTET_JOBBEN"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId2.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE")
                        .setBrukers_situasjon("ALDRI_HATT_JOBB"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId3.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("VIDEREGAENDE_GRUNNUTDANNING")
                        .setBrukers_situasjon("MISTET_JOBBEN"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId4.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning("GRUNNSKOLE"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId5.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_godkjent("JA"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId6.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning_bestatt("JA"),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId7.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet),

                new OppfolgingsBruker()
                        .setAktoer_id(aktoerId8.get())
                        .setOppfolging(true)
                        .setEnhet_id(enhet)
                        .setUtdanning("INGEN_SVAR")
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("JA")
                        .setBrukers_situasjon("ALDRI_HATT_JOBB")
        );

        brukere.forEach(bruker -> {
                    populateOpensearch(NavKontor.of(enhet), VeilederId.of(null), bruker.getAktoer_id());
                    indexer.syncronIndekseringsRequest(bruker);
                }
        );

    }
}
