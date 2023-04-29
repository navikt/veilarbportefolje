package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtak;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakApiDto;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import no.nav.pto_schema.kafka.json.topic.SisteOppfolgingsperiodeV1;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal.BEHOLDE_ARBEID;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe.STANDARD_INNSATS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = ApplicationConfigTest.class)
class OppfolgingStartetOgAvsluttetServiceTest extends EndToEndTest {

    @Autowired
    private OppfolgingPeriodeService oppfolgingPeriodeService;

    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Siste14aVedtakService siste14aVedtakService;

    @Autowired
    private Siste14aVedtakRepository siste14aVedtakRepository;

    @Autowired
    private PdlIdentRepository pdlIdentRepository;

    @Autowired
    private PdlPersonRepository pdlPersonRepository;

    @MockBean
    private PdlPortefoljeClient pdlPortefoljeClient;

    @MockBean
    private VedtaksstotteClient vedtaksstotteClient;

    @Test
    void når_oppfolging_startes_skal_bruker_settes_under_oppfølging_i_databasen() {
        final AktorId aktorId = randomAktorId();
        final Fnr fnr = TestDataUtils.randomFnr();

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        final BrukerOppdatertInformasjon info = oppfolgingRepositoryV2.hentOppfolgingData(aktorId).orElseThrow();
        assertThat(info.getOppfolging()).isTrue();
        assertThat(info.getNyForVeileder()).isFalse();
    }

    @Test
    void når_oppfolging_startes_skal_brukeridenter_hentes_og_lagres() {
        final AktorId aktorId = TestDataUtils.randomAktorId();
        final Fnr fnr = TestDataUtils.randomFnr();

        mockPdlIdenterRespons(aktorId, fnr);
        PDLPerson pdlPerson = mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);


        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        List<String> lagredeIdenter = pdlIdentRepository.hentIdenterForBruker(aktorId.get()).identer();
        assertThat(lagredeIdenter).containsExactlyInAnyOrderElementsOf(List.of(aktorId.get(), fnr.get()));
        PDLPerson pdlPersonFraDB = pdlPersonRepository.hentPerson(fnr);
        assertThat(pdlPersonFraDB.getFoedsel()).isEqualTo(pdlPerson.getFoedsel());
        assertThat(pdlPersonFraDB.getFornavn()).isEqualTo(pdlPerson.getFornavn());
        assertThat(pdlPersonFraDB.getEtternavn()).isEqualTo(pdlPerson.getEtternavn());
        assertThat(pdlPersonFraDB.getBydelsnummer()).isEqualTo(pdlPerson.getBydelsnummer());
        assertThat(pdlPersonFraDB.getDiskresjonskode()).isEqualTo(pdlPerson.getDiskresjonskode());
        assertThat(pdlPersonFraDB.getSikkerhetstiltak()).isEqualTo(pdlPerson.getSikkerhetstiltak());
        assertThat(pdlPersonFraDB.getStatsborgerskap()).isEqualTo(pdlPerson.getStatsborgerskap());
    }

    @Test
    void når_oppfolging_startes_skal_siste_14a_vedtak_hentes_og_lagres() {
        final AktorId aktorId = TestDataUtils.randomAktorId();
        final Fnr fnr = TestDataUtils.randomFnr();

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);

        Siste14aVedtakApiDto siste14aVedtakApiDto = new Siste14aVedtakApiDto(
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Hovedmal.OKE_DELTAKELSE,
                tilfeldigDatoTilbakeITid(),
                true
        );
        when(vedtaksstotteClient.hentSiste14aVedtak(fnr)).thenReturn(Optional.of(siste14aVedtakApiDto));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        IdenterForBruker identerForBruker = pdlIdentRepository.hentIdenterForBruker(aktorId.get());
        Optional<Siste14aVedtak> siste14aVedtak = siste14aVedtakRepository.hentSiste14aVedtak(identerForBruker);
        assertThat(siste14aVedtak).isNotEmpty();
        assertThat(siste14aVedtak).isEqualTo(Optional.of(Siste14aVedtak.fraApiDto(siste14aVedtakApiDto, aktorId.get())));
    }

    @Test
    void når_oppfølging_avsluttes_skal_arbeidsliste_registrering_og_oppfølgingsdata_slettes() {
        final AktorId aktoerId = randomAktorId();

        testDataClient.setupBrukerMedArbeidsliste(
                aktoerId,
                randomNavKontor(),
                randomVeilederId(),
                ZonedDateTime.parse("2020-12-01T00:00:00+02:00")
        );

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererAvsluttetOppfolgingsperiode(aktoerId));

        List<String> arbeidsliste =
                jdbcTemplate.queryForList(
                        "SELECT aktoerid from arbeidsliste where aktoerid = ?",
                        String.class,
                        aktoerId.get()
                );

        List<String> registrering =
                jdbcTemplate.query(
                        "select * from bruker_registrering where aktoerid = ?",
                        (r, i) -> r.getString("aktoerid"),
                        aktoerId.get()
                );

        assertThat(arbeidsliste.isEmpty()).isTrue();
        assertThat(registrering.size()).isEqualTo(0);
        assertThat(testDataClient.hentUnderOppfolgingOgAktivIdent(aktoerId)).isFalse();
        Map<String, Object> source = opensearchTestClient.fetchDocument(aktoerId).getSourceAsMap();
        assertThat(source).isNull();
    }

    @Test
    void når_oppfølging_avsluttes_skal_siste_14a_vedtak_slettes() {
        final AktorId aktorId = randomAktorId();

        testDataClient.lagreBrukerUnderOppfolging(aktorId, tilfeldigDatoTilbakeITid());

        siste14aVedtakService.lagreSiste14aVedtak(
                new Siste14aVedtak(aktorId.get(), STANDARD_INNSATS, BEHOLDE_ARBEID, tilfeldigDatoTilbakeITid(), false)
        );

        assertFalse(siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get()))).isEmpty());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererAvsluttetOppfolgingsperiode(aktorId));

        assertTrue(siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get()))).isEmpty());
    }

    @Test
    void skal_ikke_avslutte_bruker_som_har_startdato_senere_enn_sluttdato() {
        final AktorId aktorId = randomAktorId();
        final Fnr fnr = TestDataUtils.randomFnr();

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);

        ZonedDateTime sluttDato = tilfeldigDatoTilbakeITid();

        SisteOppfolgingsperiodeV1 periode = genererStartetOppfolgingsperiode(aktorId, tilfeldigSenereDato(sluttDato));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(periode);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(
                genererSluttdatoForOppfolgingsperiode(periode, sluttDato)
        );

        Optional<BrukerOppdatertInformasjon> bruker = oppfolgingRepositoryV2.hentOppfolgingData(aktorId);

        assertThat(bruker.orElseThrow().getOppfolging()).isTrue();
    }

    @Test
    void skal_avslutte_bruker_som_har_en_tidligere_startdato_enn_sluttdato() {
        final AktorId aktorId = randomAktorId();
        final Fnr fnr = TestDataUtils.randomFnr();

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);

        SisteOppfolgingsperiodeV1 periode = genererStartetOppfolgingsperiode(aktorId, tilfeldigDatoTilbakeITid());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(periode);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(
                genererSluttdatoForOppfolgingsperiode(periode, tilfeldigSenereDato(periode.getStartDato()))
        );

        Optional<BrukerOppdatertInformasjon> bruker = oppfolgingRepositoryV2.hentOppfolgingData(aktorId);

        assertThat(bruker).isNotPresent();
    }

    private List<PDLIdent> mockPdlIdenterRespons(AktorId aktorId, Fnr fnr) {

        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID),
                new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT)
        );

        when(pdlPortefoljeClient.hentIdenterFraPdl(aktorId)).thenReturn(identer);

        return identer;
    }

    private PDLPerson mockPdlPersonRespons(Fnr fnr) {

        String file = readFileAsJsonString("/person_pdl.json", getClass());
        PDLPerson pdlPerson = PDLPerson.genererFraApiRespons(
                JsonUtils.fromJson(file, PdlPersonResponse.class).getData().getHentPerson()
        );

        when(pdlPortefoljeClient.hentBrukerDataFraPdl(fnr)).thenReturn(pdlPerson);

        return pdlPerson;
    }

    private PDLPersonBarn mockPdlPersonBarnRespons(Fnr fnr) {

        String file = readFileAsJsonString("/person_barn_pdl.json", getClass());
        PDLPersonBarn pdlPersonBarn = PDLPersonBarn.genererFraApiRespons(
                JsonUtils.fromJson(file, PdlBarnResponse.class).getData().getHentPerson()
        );

        when(pdlPortefoljeClient.hentBrukerBarnDataFraPdl(fnr)).thenReturn(pdlPersonBarn);

        return pdlPersonBarn;
    }
}
