package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import io.getunleash.DefaultUnleash;
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
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtak;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import no.nav.pto_schema.enums.arena.Rettighetsgruppe;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static no.nav.pto.veilarbportefolje.domene.Kjonn.K;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class OppfolgingsbrukerServiceV2Test extends EndToEndTest   {
    private final JdbcTemplate db;
    private final OppfolgingsbrukerServiceV2 oppfolginsbrukerService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private final Siste14aVedtakRepository siste14aVedtakRepository;
    private final PdlIdentRepository pdlIdentRepository;
    private final PdlPersonRepository pdlPersonRepository;
    private final OpensearchService opensearchService;

    @MockBean
    private BrukerServiceV2 brukerServiceV2;
    private final Fnr fnr = randomFnr();
    final DefaultUnleash defaultUnleash = mock(DefaultUnleash.class);

    @Autowired
    public OppfolgingsbrukerServiceV2Test(JdbcTemplate db, OppfolgingsbrukerServiceV2 oppfolginsbrukerService, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3, OpensearchService opensearchService) { //, OpensearchIndexer opensearchIndexer, IndexName indexName) {
        this.db = db;
        this.oppfolginsbrukerService = oppfolginsbrukerService;
        this.oppfolgingsbrukerRepositoryV3 = oppfolgingsbrukerRepositoryV3;
        this.siste14aVedtakRepository = new Siste14aVedtakRepository(db, db);
        this.pdlIdentRepository = new PdlIdentRepository(db);
        this.pdlPersonRepository = new PdlPersonRepository(db, db);
        this.opensearchService = opensearchService;
    }

    @BeforeEach
    public void setup() {
        db.update("truncate oppfolgingsbruker_arena_v2");
        db.update("truncate bruker_identer");
  //      when(defaultUnleash.isEnabled(anyString())).thenReturn(true);
    }


    @Test
    public void skalKonsumereOgLagreData() {
        LocalDate iserv_fra_dato = ZonedDateTime.now().minusDays(2).toLocalDate();
        ZonedDateTime endret_dato = DateUtils.now();

        OppfolgingsbrukerEntity forventetResultat = new OppfolgingsbrukerEntity(fnr.get(), "RARBS", ZonedDateTime.of(iserv_fra_dato.atStartOfDay(), ZoneId.systemDefault()),
                 "007", "BKART", "AAP", "SKAFFEA", endret_dato);

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.RARBS).iservFraDato(iserv_fra_dato)
                .oppfolgingsenhet("007").kvalifiseringsgruppe(Kvalifiseringsgruppe.BKART).rettighetsgruppe(Rettighetsgruppe.AAP).hovedmaal(Hovedmaal.SKAFFEA)
                .sperretAnsatt(false).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr);
        assertTrue(oppfolgingsBruker.isPresent());
        assertThat(oppfolgingsBruker.get()).isEqualTo(forventetResultat);
    }

    @Test
    public void skalKonsumereData() {
        ZonedDateTime endret_dato = DateUtils.now();

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr.get()).formidlingsgruppe(Formidlingsgruppe.ARBS).iservFraDato(null)
                .etternavn("Testerson").fornavn("Test").oppfolgingsenhet("0220").kvalifiseringsgruppe(Kvalifiseringsgruppe.IVURD).rettighetsgruppe(Rettighetsgruppe.IYT).hovedmaal(Hovedmaal.SKAFFEA).sikkerhetstiltakType(null)
                .diskresjonskode(null).harOppfolgingssak(false).sperretAnsatt(false).erDoed(false).doedFraDato(null).sistEndretDato(endret_dato)
                .build();
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        Optional<OppfolgingsbrukerEntity> oppfolgingsBruker = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr);
        assertTrue(oppfolgingsBruker.isPresent());
    }
    @Test
    public void skalHenteHovedmalFraSiste14avedtak() {
        when(defaultUnleash.isEnabled(anyString())).thenReturn(true);
        AktorId aktorId1 = randomAktorId();
        IdenterForBruker identer = new IdenterForBruker(List.of(aktorId1.get()));
        Fnr fnr_1 = Fnr.of("16058211111");
        OppfolgingsBruker oppfolgingsBruker = new OppfolgingsBruker();
        oppfolgingsBruker.setFnr(fnr_1.get());
        oppfolgingsBruker.setHovedmaalkode(null);
        oppfolgingsBruker.setEnhet_id("0220");
        oppfolgingsBruker.setOppfolging(true);
        oppfolgingsBruker.setAktoer_id(aktorId1.get());
        opensearchIndexer.skrivBulkTilIndeks(indexName.getValue(), List.of(oppfolgingsBruker));
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(emptyList());
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId1, ZonedDateTime.now());
        when(brukerServiceV2.hentAktorId(fnr_1)).thenReturn(Optional.of(aktorId1));
        List<PDLIdent> identList = List.of(
                new PDLIdent(fnr_1.get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(aktorId1.get(), false, AKTORID));
        pdlPersonRepository.upsertPerson(fnr_1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identList);
        siste14aVedtakRepository.upsert(new Siste14aVedtak(aktorId1.get(), Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.BEHOLDE_ARBEID, ZonedDateTime.now(), false), identer);
        ZonedDateTime endret_dato = DateUtils.now();

        EndringPaaOppfoelgingsBrukerV2 kafkaMelding = EndringPaaOppfoelgingsBrukerV2.builder().fodselsnummer(fnr_1.get()).formidlingsgruppe(Formidlingsgruppe.ARBS).iservFraDato(null)
                .etternavn("Testerson").fornavn("Test").oppfolgingsenhet("0220").kvalifiseringsgruppe(Kvalifiseringsgruppe.IVURD).rettighetsgruppe(Rettighetsgruppe.IYT).hovedmaal(null).sikkerhetstiltakType(null)
                .diskresjonskode(null).harOppfolgingssak(false).sperretAnsatt(false).erDoed(false).doedFraDato(null).sistEndretDato(endret_dato)
                .build();
     //   pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);
     //   BrukereMedAntall brukereMedAntall = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt",new Filtervalg(), null, null);
     //   assertThat(brukereMedAntall.getBrukere().size()).isEqualTo(1);
        oppfolginsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
//        brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId1));
        Optional<OppfolgingsbrukerEntity> oppfolgingsBrukerEntity = oppfolgingsbrukerRepositoryV3.getOppfolgingsBruker(fnr_1);
        Optional<Siste14aVedtak> siste14aVedtak = siste14aVedtakRepository.hentSiste14aVedtak(identer);
        filtervalg.setHovedmal(List.of(ArenaHovedmal.BEHOLDEA));
        pollOpensearchUntil(() -> opensearchTestClient.countDocuments() == 1);
        BrukereMedAntall oe = opensearchService.hentBrukere("0220", Optional.empty(), "asc", "ikke_satt",filtervalg , null, null);
        assertThat(oe.getBrukere().size()).isEqualTo(1);
        assertTrue(siste14aVedtak.isPresent());
        assertTrue(oppfolgingsBrukerEntity.isPresent());

    }

}
