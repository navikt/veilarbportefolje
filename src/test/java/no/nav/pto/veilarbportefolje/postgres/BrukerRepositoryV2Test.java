package no.nav.pto.veilarbportefolje.postgres;

import io.getunleash.DefaultUnleash;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappRepository;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkService;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.IdenterForBruker;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtak;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.domene.Kjonn.K;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrukerRepositoryV2Test {
    private BrukerRepositoryV2 brukerRepositoryV2;
    private PdlIdentRepository pdlIdentRepository;
    private PdlPersonRepository pdlPersonRepository;
    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    private HuskelappRepository huskelappRepository;
    private Siste14aVedtakRepository siste14aVedtakRepository;

    @BeforeEach
    public void setUp() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        final DefaultUnleash defaultUnleash = mock(DefaultUnleash.class);
        final KodeverkService kodeverkService = mock(KodeverkService.class);
        when(defaultUnleash.isEnabled(anyString())).thenReturn(true);
        this.brukerRepositoryV2 = new BrukerRepositoryV2(db, kodeverkService);
        this.pdlIdentRepository = new PdlIdentRepository(db);
        this.oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);
        this.pdlPersonRepository = new PdlPersonRepository(db, db);
        this.oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(db, null);
        this.huskelappRepository = new HuskelappRepository(db, null);
        this.siste14aVedtakRepository = new Siste14aVedtakRepository(db, db);
    }

    @Test
    public void skalKunHenteAktivHuskelapp() {
        Fnr fnr_1 = Fnr.of("3");
        AktorId aktorId = randomAktorId();
        List<PDLIdent> identer = List.of(
                new PDLIdent(fnr_1.get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, AKTORID)
        );

        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        pdlPersonRepository.upsertPerson(fnr_1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identer);
        HuskelappOpprettRequest huskelapp1 = new HuskelappOpprettRequest(fnr_1, LocalDate.of(2024, 2, 10), "Husk nr 1", EnhetId.of("0000"));
        UUID huskelappUUID = huskelappRepository.opprettHuskelapp(huskelapp1, VeilederId.of("Z123456"));
        HuskelappRedigerRequest huskelapp2 = new HuskelappRedigerRequest(huskelappUUID, fnr_1, LocalDate.of(2026, 1, 1), "Husk nr 2", EnhetId.of("0000"));
        huskelappRepository.redigerHuskelapp(huskelapp2, VeilederId.of("Z123456"));


        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr_1.get(), null, null,
                        "0000", null, null,
                        null, ZonedDateTime.now()));
        List<OppfolgingsBruker> oppfolgingsBrukerMedHuskelapp = brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId));


        Assertions.assertThat(oppfolgingsBrukerMedHuskelapp.size()).isEqualTo(1);
        Assertions.assertThat(oppfolgingsBrukerMedHuskelapp.get(0).getFnr()).isEqualTo(fnr_1.get());
        Assertions.assertThat(oppfolgingsBrukerMedHuskelapp.get(0).getEnhet_id()).isEqualTo("0000");
        Assertions.assertThat(oppfolgingsBrukerMedHuskelapp.get(0).getHuskelapp().frist()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    public void skalBrukeIdentenSomEksistererIArena() {
        Fnr fnr_1 = Fnr.of("1");
        Fnr fnr_2 = Fnr.of("2");
        Fnr fnr_ny = Fnr.of("3");
        AktorId aktorId = randomAktorId();
        List<PDLIdent> identer = List.of(
                new PDLIdent(fnr_1.get(), true, FOLKEREGISTERIDENT),
                new PDLIdent(fnr_2.get(), true, FOLKEREGISTERIDENT),
                new PDLIdent(fnr_ny.get(), false, FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, AKTORID)
        );
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        pdlPersonRepository.upsertPerson(fnr_ny, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identer);

        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr_1.get(), null, null,
                        "0000", null, null,
                        null, ZonedDateTime.now().minusDays(1)));
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr_2.get(), null, null,
                        "0000", null, null,
                        null, ZonedDateTime.now()));
        List<OppfolgingsBruker> oppfolgingsBrukers_pre_nyFnrIArena = brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId));

        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr_ny.get(), null, null,
                        "0001", null, null,
                        null, ZonedDateTime.now()));
        List<OppfolgingsBruker> oppfolgingsBrukers_post_nyFnrIArena = brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId));


        Assertions.assertThat(oppfolgingsBrukers_pre_nyFnrIArena.size()).isEqualTo(1);
        Assertions.assertThat(oppfolgingsBrukers_post_nyFnrIArena.size()).isEqualTo(1);
        Assertions.assertThat(oppfolgingsBrukers_pre_nyFnrIArena.get(0).getEnhet_id()).isEqualTo("0000");
        Assertions.assertThat(oppfolgingsBrukers_pre_nyFnrIArena.get(0).getFnr()).isEqualTo(fnr_2.get());
        Assertions.assertThat(oppfolgingsBrukers_post_nyFnrIArena.get(0).getFnr()).isEqualTo(fnr_ny.get());
        Assertions.assertThat(oppfolgingsBrukers_post_nyFnrIArena.get(0).getEnhet_id()).isEqualTo("0001");
    }
    @Test
    public void skalHenteHovedmalFraRiktigSted() {
        IdenterForBruker identer = new IdenterForBruker(List.of("1"));
        Fnr fnr_1 = Fnr.of("1");
        Fnr fnr_2 = Fnr.of("2");
        Fnr fnr_ny = Fnr.of("3");
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        AktorId aktorId3 = randomAktorId();
        List<PDLIdent> identList = List.of(
                new PDLIdent(fnr_1.get(), false, FOLKEREGISTERIDENT),
        new PDLIdent(aktorId1.get(), false, AKTORID));
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId1, ZonedDateTime.now());
        pdlPersonRepository.upsertPerson(fnr_1, new PDLPerson().setKjonn(K).setFoedsel(LocalDate.now()));
        pdlIdentRepository.upsertIdenter(identList);
        oppfolgingsbrukerRepositoryV3.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr_1.get(), null, null,
                        "0321", "BATT", null,
                        "SKAFFEA", ZonedDateTime.now()));
        siste14aVedtakRepository.upsert(new Siste14aVedtak(aktorId1.get(), Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Hovedmal.BEHOLDE_ARBEID, ZonedDateTime.now(), false), identer);
        brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId1));
        String kode = brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId1)).get(0).getHovedmaalkode();
        Assertions.assertThat(brukerRepositoryV2.hentOppfolgingsBrukere(List.of(aktorId1)).get(0).getHovedmaalkode()).isEqualTo("BEHOLDEA");

    }


}
