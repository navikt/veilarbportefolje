package no.nav.pto.veilarbportefolje.oppfolging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aap.AapClient;
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerPeriodeEntity;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssoekerService;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ArbeidssokerperiodeResponse;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerEntity;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerJobbsituasjonEntity;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OpplysningerOmArbeidssoekerResponse;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OppslagArbeidssoekerregisteretClient;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ProfileringEntity;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.ProfileringResponse;
import no.nav.pto.veilarbportefolje.client.AktorClient;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerClient;
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPerioderResponseDto;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.oppfolging.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerDTO;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerServiceV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerTestRepository;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.VeilarbarenaClient;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.gjeldende14aVedtak.GjeldendeVedtak14a;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakApiDto;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakForBruker;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakService;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerClient;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataClient;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import no.nav.pto_schema.enums.arena.Formidlingsgruppe;
import no.nav.pto_schema.enums.arena.Hovedmaal;
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.SerialiseringOgDeserialiseringUtilsKt.getObjectMapper;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.genererAvsluttetOppfolgingsperiode;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.genererSluttdatoForOppfolgingsperiode;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.genererStartetOppfolgingsperiode;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.tilfeldigDatoTilbakeITid;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.tilfeldigSenereDato;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal.BEHOLDE_ARBEID;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe.STANDARD_INNSATS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Autowired
    private AktorClient aktorClient;

    @Autowired
    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;

    @Autowired
    private OppfolgingsbrukerTestRepository oppfolgingsbrukerTestRepository;

    @Autowired
    private OppfolgingsbrukerServiceV2 oppfolgingsbrukerService;

    @Autowired
    private ArbeidssoekerService arbeidssoekerService;

    @Autowired
    private AapClient aapClient;

    @Autowired
    private TiltakspengerClient tiltakspengerClient;

    @Autowired
    private DagpengerClient dagpengerClient;

    @MockBean
    private PdlPortefoljeClient pdlPortefoljeClient;

    @MockBean
    private VedtaksstotteClient vedtaksstotteClient;

    @MockBean
    private VeilarbarenaClient veilarbarenaClient;

    @MockBean
    private OppslagArbeidssoekerregisteretClient oppslagArbeidssoekerregisteretClient;

    private final Fnr fnr = Fnr.of("17858998980");
    private final AktorId aktorId = randomAktorId();


    @BeforeEach
    public void cleanup() {
        jdbcTemplate.update("truncate bruker_data CASCADE");
        jdbcTemplate.update("truncate bruker_data_barn CASCADE");
        jdbcTemplate.update("truncate foreldreansvar");
        jdbcTemplate.update("truncate siste_arbeidssoeker_periode cascade");
    }

    @Test
    void nar_oppfolging_startes_skal_bruker_settes_under_oppfolging_i_databasen() {
        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        final BrukerOppdatertInformasjon info = oppfolgingRepositoryV2.hentOppfolgingData(aktorId).orElseThrow();
        assertThat(info.getOppfolging()).isTrue();
        assertThat(info.getNyForVeileder()).isFalse();
    }

    @Test
    void nar_ny_melding_om_oppfolging_start_kommer_skal_oppdatert_kontor_lagres() {
        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        var startDato = ZonedDateTime.now().minusDays(1);
        var oppfolgingsperiodeId = UUID.randomUUID();
        var forsteKontor = "1234";
        var andreKontor = "4321";
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId, startDato, oppfolgingsperiodeId, forsteKontor, fnr));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId, startDato, oppfolgingsperiodeId, andreKontor, fnr));

        var oppfolgingsbruker = oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr).get();
        assertThat(oppfolgingsbruker.nav_kontor()).isEqualTo(andreKontor);
    }

    @Test
    void nar_oppfolging_startes_skal_brukeridenter_hentes_og_lagres() {
        mockPdlIdenterRespons(aktorId, fnr);
        PDLPerson pdlPerson = mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

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
        assertTrue(pdlPersonFraDB.getStatsborgerskap().containsAll(pdlPerson.getStatsborgerskap()));
    }

    @Test
    void nar_oppfolging_startes_skal_siste_14a_vedtak_hentes_og_lagres() {
        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        var sisteOppfolgingsperiodeV1 = genererStartetOppfolgingsperiode(aktorId);
        Siste14aVedtakApiDto siste14aVedtakApiDto = new Siste14aVedtakApiDto(
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Hovedmal.OKE_DELTAKELSE,
                sisteOppfolgingsperiodeV1.getStartTidspunkt().plusDays(1),
                true
        );
        when(vedtaksstotteClient.hentSiste14aVedtak(fnr)).thenReturn(Optional.of(siste14aVedtakApiDto));
        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(sisteOppfolgingsperiodeV1);

        IdenterForBruker identerForBruker = pdlIdentRepository.hentIdenterForBruker(aktorId.get());
        Optional<Siste14aVedtakForBruker> siste14aVedtakForBruker = siste14aVedtakRepository.hentSiste14aVedtak(identerForBruker);
        assertThat(siste14aVedtakForBruker).isNotEmpty();
        assertThat(siste14aVedtakForBruker).isEqualTo(Optional.of(Siste14aVedtakForBruker.fraApiDto(siste14aVedtakApiDto, aktorId)));

        GjeldendeVedtak14a gjeldendeVedtak14aFraOpenSearch = opensearchTestClient.hentBrukerFraOpensearch(aktorId).getGjeldendeVedtak14a();
        GjeldendeVedtak14a forventetGjeldendeVedtak14a = new GjeldendeVedtak14a(
                siste14aVedtakApiDto.getInnsatsgruppe(),
                siste14aVedtakApiDto.getHovedmal(),
                // Vi må kvitte oss med ZoneId siden dates lagret i OpenSearch ikke får med dette (kun tidssone).
                // Derfor gjør vi denne toOffsetDataTime().toZonedDateTime() "hacken".
                siste14aVedtakApiDto.getFattetDato().toOffsetDateTime().toZonedDateTime()
        );
        assertThat(gjeldendeVedtak14aFraOpenSearch).isNotNull();
        assertThat(gjeldendeVedtak14aFraOpenSearch).isEqualTo(forventetGjeldendeVedtak14a);
    }

    @Test
    void nar_oppfolging_startes_skal_oppfolgingsbrukerdata_hentes_og_lagres() {
        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockSiste14aVedtakResponse(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        Optional<OppfolgingsbrukerEntity> oppfolgingsbrukerEntity = oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr);
        assertThat(oppfolgingsbrukerEntity).isPresent();

    }

    @Test
    void nar_oppfolging_startes_skal_oppfolgingsbrukerdata_hentes_og_oppdateres_naar_vi_har_oppfolgingsbruker_data_fra_foer() {
        insertOppfolgingsbrukerEntity(ZonedDateTime.parse("2024-04-04T00:00:00+02:00").minusDays(2));

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockSiste14aVedtakResponse(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        Optional<OppfolgingsbrukerEntity> oppfolgingsbrukerEntity = oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr);
        assertThat(oppfolgingsbrukerEntity).isPresent();
        assertThat(oppfolgingsbrukerEntity.get().endret_dato()).isEqualTo(ZonedDateTime.parse("2024-04-04T00:00:00+02:00"));
    }

    @Test
    void nar_oppfolging_startes_skal_oppfolgingsbrukerdata_hentes_og_ignoreres_naar_vi_har_oppfolgingsbruker_data_fra_foer() {
        insertOppfolgingsbrukerEntity(ZonedDateTime.parse("2024-04-04T00:00:00+02:00").plusDays(2));

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockSiste14aVedtakResponse(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        Optional<OppfolgingsbrukerEntity> oppfolgingsbrukerEntity = oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr);
        assertThat(oppfolgingsbrukerEntity).isPresent();
        assertThat(oppfolgingsbrukerEntity.get().endret_dato()).isEqualTo(ZonedDateTime.parse("2024-04-04T00:00:00+02:00").plusDays(2));
    }

    @Test
    void nar_oppfolging_startes_skal_arbeidssoekerdata_hentes_lagres() throws JsonProcessingException {
        UUID periodeId = UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16");
        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockSiste14aVedtakResponse(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentArbeidssoekerPerioderResponse(fnr);
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, periodeId);
        mockHentProfileringResponse(fnr, periodeId);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererStartetOppfolgingsperiode(aktorId));

        ArbeidssoekerPeriodeEntity arbeidssoekerPeriode = TestDataClient.getArbeidssoekerPeriodeFraDb(jdbcTemplate, periodeId);

        assertNotNull(arbeidssoekerPeriode);
        OpplysningerOmArbeidssoekerEntity opplysningerOmArbeidssoeker = TestDataClient.getOpplysningerOmArbeidssoekerFraDb(jdbcTemplate, arbeidssoekerPeriode.getArbeidssoekerperiodeId());

        assertNotNull(opplysningerOmArbeidssoeker);
        OpplysningerOmArbeidssoekerJobbsituasjonEntity opplysningerOmArbeidssoekerJobbsituasjon = TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(jdbcTemplate, opplysningerOmArbeidssoeker.getOpplysningerOmArbeidssoekerId());

        assertNotNull(opplysningerOmArbeidssoekerJobbsituasjon);
        assertThat(opplysningerOmArbeidssoekerJobbsituasjon.getJobbsituasjon().get(1)).isEqualTo(JobbSituasjonBeskrivelse.ER_PERMITTERT.name());

        ProfileringEntity profilering = TestDataClient.getProfileringFraDb(jdbcTemplate, periodeId);
        assertNotNull(profilering);
    }


    @Test
    void nar_oppfolging_avsluttes_skal_siste_14a_vedtak_slettes() {
        when(aktorClient.hentFnr(aktorId)).thenReturn(randomFnr());
        when(aktorClient.hentAktorId(any())).thenReturn(aktorId);

        testDataClient.lagreBrukerUnderOppfolging(aktorId, tilfeldigDatoTilbakeITid());

        siste14aVedtakService.lagreSiste14aVedtak(
                new Siste14aVedtakForBruker(aktorId, STANDARD_INNSATS, BEHOLDE_ARBEID, tilfeldigDatoTilbakeITid(), false)
        );

        assertFalse(siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get()))).isEmpty());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererAvsluttetOppfolgingsperiode(aktorId));

        assertTrue(siste14aVedtakRepository.hentSiste14aVedtak(new IdenterForBruker(List.of(aktorId.get()))).isEmpty());
    }

    @Test
    void nar_oppfolging_avsluttes_skal_oppfolgingsbrukerdata_slettes() {
        when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        mockHentOppfolgingsbrukerResponse(fnr);

        testDataClient.lagreBrukerUnderOppfolging(aktorId, fnr);

        oppfolgingsbrukerService.hentOgLagreOppfolgingsbruker(aktorId, new NavKontor("0101"));

        Optional<OppfolgingsbrukerEntity> oppfolgingsbrukerEntityForAvsluttet = oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr);
        assertThat(oppfolgingsbrukerEntityForAvsluttet).isPresent();

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererAvsluttetOppfolgingsperiode(aktorId));

        Optional<OppfolgingsbrukerEntity> oppfolgingsbrukerEntityEtterAvsluttet = oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr);
        assertThat(oppfolgingsbrukerEntityEtterAvsluttet).isEmpty();
    }

    @Test
    void nar_oppfolging_avsluttes_skal_arbeidsokerdata_slettes() throws JsonProcessingException {
        when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        when(aktorClient.hentAktorId(fnr)).thenReturn(aktorId);
        mockHentArbeidssoekerPerioderResponse(fnr);
        mockHentOpplysningerOmArbeidssoekerResponse(fnr, UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"));

        testDataClient.lagreBrukerUnderOppfolging(aktorId, fnr);

        arbeidssoekerService.hentOgLagreArbeidssoekerdataForBruker(aktorId);

        ArbeidssoekerPeriodeEntity sisteArbeidssoekerPeriodeForAvsluttet = TestDataClient.getArbeidssoekerPeriodeFraDb(jdbcTemplate, UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"));
        assertThat(sisteArbeidssoekerPeriodeForAvsluttet).isNotNull();
        OpplysningerOmArbeidssoekerEntity opplysningerOmArbeidssoekerForAvsluttet = TestDataClient.getOpplysningerOmArbeidssoekerFraDb(jdbcTemplate, sisteArbeidssoekerPeriodeForAvsluttet.getArbeidssoekerperiodeId());
        assertThat(opplysningerOmArbeidssoekerForAvsluttet).isNotNull();
        OpplysningerOmArbeidssoekerJobbsituasjonEntity opplysningerOmArbeidssoekerJobbsituasjonForAvsluttet = TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(jdbcTemplate, opplysningerOmArbeidssoekerForAvsluttet.getOpplysningerOmArbeidssoekerId());
        assertThat(opplysningerOmArbeidssoekerJobbsituasjonForAvsluttet).isNotNull();

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(genererAvsluttetOppfolgingsperiode(aktorId));

        ArbeidssoekerPeriodeEntity sisteArbeidssoekerPeriodeEtterAvsluttet = TestDataClient.getArbeidssoekerPeriodeFraDb(jdbcTemplate, UUID.fromString("ea0ad984-8b99-4fff-afd6-07737ab19d16"));
        assertThat(sisteArbeidssoekerPeriodeEtterAvsluttet).isNull();
        OpplysningerOmArbeidssoekerEntity opplysningerOmArbeidssoekerEtterAvsluttet = TestDataClient.getOpplysningerOmArbeidssoekerFraDb(jdbcTemplate, sisteArbeidssoekerPeriodeForAvsluttet.getArbeidssoekerperiodeId());
        assertThat(opplysningerOmArbeidssoekerEtterAvsluttet).isNull();
        OpplysningerOmArbeidssoekerJobbsituasjonEntity opplysningerOmArbeidssoekerJobbsituasjonEtterAvsluttet = TestDataClient.getOpplysningerOmArbeidssoekerJobbsituasjonFraDb(jdbcTemplate, opplysningerOmArbeidssoekerForAvsluttet.getOpplysningerOmArbeidssoekerId());
        assertThat(opplysningerOmArbeidssoekerJobbsituasjonEtterAvsluttet).isNull();
    }

    @Test
    void skal_ikke_avslutte_bruker_som_har_startdato_senere_enn_sluttdato() {
        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        ZonedDateTime sluttDato = tilfeldigDatoTilbakeITid();

        var periode = genererStartetOppfolgingsperiode(aktorId, tilfeldigSenereDato(sluttDato));

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(periode);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(
                genererSluttdatoForOppfolgingsperiode(periode, sluttDato)
        );

        Optional<BrukerOppdatertInformasjon> bruker = oppfolgingRepositoryV2.hentOppfolgingData(aktorId);

        assertThat(bruker.orElseThrow().getOppfolging()).isTrue();
    }

    @Test
    void skal_avslutte_bruker_som_har_en_tidligere_startdato_enn_sluttdato() {
        when(aktorClient.hentFnr(aktorId)).thenReturn(randomFnr());
        when(aktorClient.hentAktorId(randomFnr())).thenReturn(aktorId);

        mockPdlIdenterRespons(aktorId, fnr);
        mockPdlPersonRespons(fnr);
        mockPdlPersonBarnRespons(fnr);
        mockHentOppfolgingsbrukerResponse(fnr);
        mockHentAapResponse(fnr);
        mockHentTiltakspengerResponse(fnr);
        mockHentDagpengerResponse(fnr);

        var periode = genererStartetOppfolgingsperiode(aktorId, tilfeldigDatoTilbakeITid());

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(periode);

        oppfolgingPeriodeService.behandleKafkaMeldingLogikk(
                genererSluttdatoForOppfolgingsperiode(periode, tilfeldigSenereDato(periode.getStartTidspunkt()))
        );

        Optional<BrukerOppdatertInformasjon> bruker = oppfolgingRepositoryV2.hentOppfolgingData(aktorId);

        assertThat(bruker).isNotPresent();
    }

    private void mockPdlIdenterRespons(AktorId aktorId, Fnr fnr) {

        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID),
                new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT)
        );

        when(pdlPortefoljeClient.hentIdenterFraPdl(aktorId)).thenReturn(identer);

    }

    private PDLPerson mockPdlPersonRespons(Fnr fnr) {

        String file = readFileAsJsonString("/PDL_Files/person_pdl.json", getClass());
        PDLPerson pdlPerson = PDLPerson.genererFraApiRespons(
                JsonUtils.fromJson(file, PdlPersonResponse.class).getData().getHentPerson()
        );

        when(pdlPortefoljeClient.hentBrukerDataFraPdl(fnr)).thenReturn(pdlPerson);

        return pdlPerson;
    }

    private PDLPersonBarn mockPdlPersonBarnRespons(Fnr fnr) {

        String file = readFileAsJsonString("/PDL_Files/person_barn_pdl.json", getClass());
        PDLPersonBarn pdlPersonBarn = PDLPersonBarn.genererFraApiRespons(
                JsonUtils.fromJson(file, PdlBarnResponse.class).getData().getHentPerson()
        );

        when(pdlPortefoljeClient.hentBrukerBarnDataFraPdl(any())).thenReturn(pdlPersonBarn);

        return pdlPersonBarn;
    }

    private void mockSiste14aVedtakResponse(Fnr fnr) {
        Siste14aVedtakApiDto siste14aVedtakApiDto = new Siste14aVedtakApiDto(
                Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                Hovedmal.OKE_DELTAKELSE,
                tilfeldigDatoTilbakeITid(),
                true
        );
        when(vedtaksstotteClient.hentSiste14aVedtak(fnr)).thenReturn(Optional.of(siste14aVedtakApiDto));
    }

    private void mockHentOppfolgingsbrukerResponse(Fnr fnr) {
        String file = readFileAsJsonString("/oppfolgingsbruker.json", getClass());
        OppfolgingsbrukerDTO oppfolgingsbrukerDTO = JsonUtils.fromJson(file, OppfolgingsbrukerDTO.class);
        when(veilarbarenaClient.hentOppfolgingsbruker(fnr)).thenReturn(Optional.of(oppfolgingsbrukerDTO));
    }

    private void mockHentArbeidssoekerPerioderResponse(Fnr fnr) throws JsonProcessingException {
        String file = readFileAsJsonString("/arbeidssoekerperioder.json", getClass());
        List<ArbeidssokerperiodeResponse> arbeidssoekerResponse = getObjectMapper().readValue(file, new TypeReference<>() {
        });
        when(oppslagArbeidssoekerregisteretClient.hentArbeidssokerPerioder(fnr.get())).thenReturn(arbeidssoekerResponse);
    }

    private void mockHentOpplysningerOmArbeidssoekerResponse(Fnr fnr, UUID periodeId) throws JsonProcessingException {
        String file = readFileAsJsonString("/opplysningerOmArbeidssoeker.json", getClass());
        List<OpplysningerOmArbeidssoekerResponse> opplysningerOmArbeidssoekerResponse = getObjectMapper().readValue(file, new TypeReference<>() {
        });
        when(oppslagArbeidssoekerregisteretClient.hentOpplysningerOmArbeidssoeker(fnr.get(), periodeId)).thenReturn(opplysningerOmArbeidssoekerResponse);
    }

    private void mockHentProfileringResponse(Fnr fnr, UUID periodeId) throws JsonProcessingException {
        String file = readFileAsJsonString("/profilering.json", getClass());
        List<ProfileringResponse> profileringResponse = getObjectMapper().readValue(file, new TypeReference<>() {
        });
        when(oppslagArbeidssoekerregisteretClient.hentProfilering(fnr.get(), periodeId)).thenReturn(profileringResponse);
    }

    private void mockHentAapResponse(Fnr fnr) {
        AapVedtakResponseDto aapResponse = new AapVedtakResponseDto(Collections.emptyList());
        when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        when(aapClient.hentAapVedtak(any(), any(), any())).thenReturn(aapResponse);
    }

    private void mockHentTiltakspengerResponse(Fnr fnr) {
        when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        when(tiltakspengerClient.hentTiltakspenger(any(), any(), any())).thenReturn(Collections.emptyList());
    }

    private void mockHentDagpengerResponse(Fnr fnr) {
        DagpengerPerioderResponseDto dagpengerPerioder = new DagpengerPerioderResponseDto("", Collections.emptyList());
        when(aktorClient.hentFnr(aktorId)).thenReturn(fnr);
        when(dagpengerClient.hentDagpengerPerioder(any(), any(), any())).thenReturn(dagpengerPerioder);
        when(dagpengerClient.hentDagpengerBeregninger(any(), any(), any())).thenReturn(Collections.emptyList());
    }

    private void insertOppfolgingsbrukerEntity(ZonedDateTime endret_dato) {
        OppfolgingsbrukerEntity oppfolgingsbruker = new OppfolgingsbrukerEntity(
                fnr.get(),
                Formidlingsgruppe.ARBS.name(),
                null,
                "1234",
                Kvalifiseringsgruppe.BATT.name(),
                null,
                Hovedmaal.BEHOLDEA.name(),
                endret_dato
        );
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(oppfolgingsbruker, new NavKontor(oppfolgingsbruker.nav_kontor()));
    }
}
