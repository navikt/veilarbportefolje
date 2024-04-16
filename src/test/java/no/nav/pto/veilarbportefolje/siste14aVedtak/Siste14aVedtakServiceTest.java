package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static no.nav.pto.veilarbportefolje.domene.Kjonn.K;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
class Siste14aVedtakServiceTest extends EndToEndTest {
    private final JdbcTemplate db;
    private final Siste14aVedtakService siste14aVedtakService;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final OpensearchService opensearchService;

    @MockBean
    private BrukerServiceV2 brukerServiceV2;
    @Autowired
    public Siste14aVedtakServiceTest(JdbcTemplate db, Siste14aVedtakService siste14aVedtakService, OpensearchService opensearchService) {
        this.db = db;
        this.siste14aVedtakService = siste14aVedtakService;
        this.siste14aVedtakRepository = new Siste14aVedtakRepository(db, db);
        this.pdlIdentRepository = new PdlIdentRepository(db);
        this.pdlPersonRepository = new PdlPersonRepository(db, db);
        this.opensearchService = opensearchService;
    }
    @Test
    void skalOppdatereOpensearchMedHovedmalDersomVedtakFattetIModia() {
        AktorId aktorId = randomAktorId();
        Fnr fnr = Fnr.of("05818399326");
        IdenterForBruker identer = new IdenterForBruker(List.of(aktorId.get()));
        setupPdl(fnr, aktorId);
        setupOpensearch(fnr, aktorId, null);

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(emptyList());

        siste14aVedtakRepository.upsert(new Siste14aVedtak(aktorId.get(), Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.SKAFFE_ARBEID, ZonedDateTime.now(), true), identer);

        Siste14aVedtakKafkaDto kafkaMelding = new Siste14aVedtakKafkaDto(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.BEHOLDE_ARBEID, DateUtils.now(), false);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);
        BrukereMedAntall brukereForKafkamelding = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
        assertThat(brukereForKafkamelding.getBrukere().size()).isEqualTo(1);

        siste14aVedtakService.behandleKafkaMeldingLogikk(kafkaMelding);
        verifiserAsynkront(1, TimeUnit.SECONDS, () -> {
            filtervalg.setHovedmal(List.of(ArenaHovedmal.BEHOLDEA));
            BrukereMedAntall brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
            assertThat(brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid.getBrukere().size()).isEqualTo(1);
        });
    }

    @Test
    void skalOppdatereOpensearchMedHovedmalDersomVedtakFattetIModiaOmHovedmaalKodeErSattFraFor() throws InterruptedException {
        AktorId aktorId = randomAktorId();
        Fnr fnr = Fnr.of("05818399326");
        IdenterForBruker identer = new IdenterForBruker(List.of(aktorId.get()));
        setupPdl(fnr, aktorId);
        setupOpensearch(fnr, aktorId, ArenaHovedmal.BEHOLDEA.name());

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(emptyList());
        filtervalg.setHovedmal(List.of(ArenaHovedmal.BEHOLDEA));

        siste14aVedtakRepository.upsert(new Siste14aVedtak(aktorId.get(), Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.BEHOLDE_ARBEID, ZonedDateTime.now(), true), identer);

        Siste14aVedtakKafkaDto kafkaMelding = new Siste14aVedtakKafkaDto(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.SKAFFE_ARBEID, DateUtils.now(), false);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);
        BrukereMedAntall brukereForKafkamelding = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
        assertThat(brukereForKafkamelding.getBrukere().size()).isEqualTo(1);

        siste14aVedtakService.behandleKafkaMeldingLogikk(kafkaMelding);

        verifiserAsynkront(2, TimeUnit.SECONDS, () -> {
            filtervalg.setHovedmal(List.of(ArenaHovedmal.SKAFFEA));
            BrukereMedAntall brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
            assertThat(brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid.getBrukere().size()).isEqualTo(1);
        });
    }

    @Test
    void skalOppdatereOpensearchMedHovedmalDersomVedtakFattetIModiaSelvomHovedmaalErnull() {
        AktorId aktorId = randomAktorId();
        Fnr fnr = Fnr.of("05818399326");
        IdenterForBruker identer = new IdenterForBruker(List.of(aktorId.get()));
        setupPdl(fnr, aktorId);
        setupOpensearch(fnr, aktorId, ArenaHovedmal.BEHOLDEA.name());

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(emptyList());
        filtervalg.setHovedmal(List.of(ArenaHovedmal.BEHOLDEA));

        siste14aVedtakRepository.upsert(new Siste14aVedtak(aktorId.get(), Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.BEHOLDE_ARBEID, ZonedDateTime.now(), true), identer);

        Siste14aVedtakKafkaDto kafkaMelding = new Siste14aVedtakKafkaDto(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, null, DateUtils.now(), false);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);
        BrukereMedAntall brukereForKafkamelding = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
        assertThat(brukereForKafkamelding.getBrukere().size()).isEqualTo(1);

        siste14aVedtakService.behandleKafkaMeldingLogikk(kafkaMelding);

        verifiserAsynkront(1, TimeUnit.SECONDS, () -> {
            filtervalg.setHovedmal(emptyList());
            BrukereMedAntall brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
            assertThat(brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid.getBrukere().size()).isEqualTo(1);
        });
    }

    @Test
    void skalIkkeOppdatereOpensearchMedHovedmalDersomVedtakFattetIArena() {
        AktorId aktorId = randomAktorId();
        Fnr fnr = Fnr.of("05818399326");
        IdenterForBruker identer = new IdenterForBruker(List.of(aktorId.get()));
        setupPdl(fnr, aktorId);
        setupOpensearch(fnr, aktorId, ArenaHovedmal.BEHOLDEA.name());

        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(emptyList());
        filtervalg.setHovedmal(List.of(ArenaHovedmal.BEHOLDEA));

        siste14aVedtakRepository.upsert(new Siste14aVedtak(aktorId.get(), Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.BEHOLDE_ARBEID, ZonedDateTime.now(), true), identer);

        Siste14aVedtakKafkaDto kafkaMelding = new Siste14aVedtakKafkaDto(aktorId, Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.SKAFFE_ARBEID, DateUtils.now(), true);

        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);
        BrukereMedAntall brukereForKafkamelding = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
        assertThat(brukereForKafkamelding.getBrukere().size()).isEqualTo(1);

        siste14aVedtakService.behandleKafkaMeldingLogikk(kafkaMelding);

        verifiserAsynkront(1, TimeUnit.SECONDS, () -> {
            BrukereMedAntall brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt", filtervalg, null, null);
            assertThat(brukereEtterkafkameldingFiltrertPaaHovedmalBeholdeArbeid.getBrukere().size()).isEqualTo(1);
        });
    }

    private void setupPdl(Fnr fnr, AktorId aktorId) {
        when(brukerServiceV2.hentAktorId(fnr)).thenReturn(Optional.of(aktorId));
        List<PDLIdent> identList = List.of(
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, AKTORID));
        pdlPersonRepository.upsertPerson(fnr, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identList);
    }

    private void setupOpensearch(Fnr fnr, AktorId aktorId, String arenahovedmalkode) {
        OppfolgingsBruker oppfolgingsBruker = new OppfolgingsBruker();
        oppfolgingsBruker.setFnr(fnr.get());
        oppfolgingsBruker.setHovedmaalkode(arenahovedmalkode);
        oppfolgingsBruker.setEnhet_id("0220");
        oppfolgingsBruker.setOppfolging(true);
        oppfolgingsBruker.setAktoer_id(aktorId.get());
        oppfolgingsBruker.setRettighetsgruppekode(Rettighetsgruppe.IYT.name());
        oppfolgingsBruker.setKvalifiseringsgruppekode(Kvalifiseringsgruppe.IVURD.name());

        opensearchIndexer.skrivBulkTilIndeks(indexName.getValue(), List.of(oppfolgingsBruker));
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
    }
}