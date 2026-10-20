package no.nav.pto.veilarbportefolje.huskelapp;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.client.AktorClient;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;

public class HuskelappServiceTest {

    private final HuskelappService huskelappService;
    private final JdbcTemplate jdbcTemplate;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final PdlIdentRepository pdlIdentRepository;
    private final BrukerServiceV2 brukerServiceV2;

    public HuskelappServiceTest() {
        jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate();
        this.pdlIdentRepository = new PdlIdentRepository(jdbcTemplate);
        OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(jdbcTemplate, new NamedParameterJdbcTemplate(jdbcTemplate));
        this.oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(jdbcTemplate);
        this.brukerServiceV2 = new BrukerServiceV2(new PdlIdentRepository(jdbcTemplate), oppfolgingsbrukerRepositoryV3, oppfolgingRepositoryV2, Mockito.mock(AktorClient.class));
        huskelappService = new HuskelappService(Mockito.mock(OpensearchIndexerPaDatafelt.class), brukerServiceV2, new HuskelappRepository(jdbcTemplate, jdbcTemplate), pdlIdentRepository);
    }

    @BeforeEach
    public void setUp() {
        this.jdbcTemplate.execute("TRUNCATE TABLE HUSKELAPP");
        this.jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        this.jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        this.jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
        this.jdbcTemplate.execute("TRUNCATE TABLE ao_kontor");
    }

    @Test
    public void testRedigereOgHenteHuskelapp() {
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        EnhetId enhetId = EnhetId.of("0110");
        VeilederId veilederId1 = VeilederId.of("1111");

        insertOppfolgingsInformasjon(fnr1, aktorId1, veilederId1, enhetId, oppfolgingsperiodeId);
        insertOppfolgingsInformasjon(fnr2, aktorId2, veilederId1, enhetId, oppfolgingsperiodeId);

        HuskelappOpprettRequest huskelapp1 = new HuskelappOpprettRequest(fnr1,
                LocalDate.of(2026, 1, 1), ("Huskelapp nr.1 sin kommentar"));

        HuskelappOpprettRequest huskelapp2 = new HuskelappOpprettRequest(fnr2,
                LocalDate.of(2017, 2, 27), ("Huskelapp nr.2 sin kommentar"));

        huskelappService.opprettHuskelapp(huskelapp1, veilederId1);
        huskelappService.opprettHuskelapp(huskelapp2, veilederId1);

        LocalDate nyFrist = LocalDate.of(2025, 10, 11);
        Optional<Huskelapp> huskelapp1result = huskelappService.hentHuskelapp(huskelapp1.brukerFnr());
        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelapp1result.get().huskelappId(), huskelapp1.brukerFnr(), nyFrist, "ny kommentar på huskelapp nr.1");
        huskelappService.redigerHuskelapp(huskelappRedigerRequest, veilederId1);

        Optional<Huskelapp> result1 = huskelappService.hentHuskelapp(fnr1);
        assertThat(result1.isPresent()).isTrue();
        Optional<Huskelapp> result2 = huskelappService.hentHuskelapp(fnr2);
        assertThat(result2.isPresent()).isTrue();
    }

    @Test
    public void skalKunHenteAktivHuskelapp() {
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        UUID oppfolgingsperiodeId = UUID.randomUUID();
        EnhetId enhetId = EnhetId.of("0110");
        VeilederId veilederId1 = VeilederId.of("1111");
        VeilederId veilederId2 = VeilederId.of("2222");

        insertOppfolgingsInformasjon(fnr1, aktorId1, veilederId1, enhetId, oppfolgingsperiodeId);
        insertOppfolgingsInformasjon(fnr2, aktorId2, veilederId2, enhetId, oppfolgingsperiodeId);

        HuskelappOpprettRequest huskelapp1 = new HuskelappOpprettRequest(fnr1, LocalDate.of(2024, 2, 10), "Husk nr 1");
        UUID huskelappUUID = huskelappService.opprettHuskelapp(huskelapp1, veilederId1);
        HuskelappRedigerRequest huskelapp2 = new HuskelappRedigerRequest(huskelappUUID, fnr1, LocalDate.of(2026, 1, 1), "Husk nr 2");
        huskelappService.redigerHuskelapp(huskelapp2, veilederId2);

        Optional<Huskelapp> result1 = huskelappService.hentHuskelapp(fnr1);
        assertThat(result1.isPresent()).isTrue();
    }

    @Test
    public void skalHenteOgDeaktivereHuskelappPaaHistoriskFnr() {
        Fnr historiskFnr = randomFnr();
        Fnr gjeldendeFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        EnhetId enhetId = EnhetId.of("0110");
        VeilederId veilederId = VeilederId.of("1111");

        insertOppfolgingsInformasjon(historiskFnr, aktorId, veilederId, enhetId, UUID.randomUUID());
        UUID gammelHuskelappId = huskelappService.opprettHuskelapp(new HuskelappOpprettRequest(historiskFnr, LocalDate.of(2026, 1, 1), "Gammel"), veilederId);

        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(historiskFnr.get(), true, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(gjeldendeFnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));

        Optional<Huskelapp> huskelappPaaGjeldendeFnr = huskelappService.hentHuskelapp(gjeldendeFnr);
        assertThat(huskelappPaaGjeldendeFnr).isPresent();
        assertThat(huskelappPaaGjeldendeFnr.get().huskelappId()).isEqualTo(gammelHuskelappId);

        UUID nyHuskelappId = huskelappService.opprettHuskelapp(new HuskelappOpprettRequest(gjeldendeFnr, LocalDate.of(2026, 2, 1), "Ny"), veilederId);

        Integer antallAktive = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HUSKELAPP WHERE STATUS = 'AKTIV'", Integer.class);
        assertThat(antallAktive).isEqualTo(1);
        assertThat(huskelappService.hentHuskelapp(gjeldendeFnr).map(Huskelapp::huskelappId)).contains(nyHuskelappId);
    }

    @Test
    public void skalPrioritereHuskelappPaaAktivIdentFoerEndretDatoVedFlereAktiveHuskelapper() {
        Fnr historiskFnr = randomFnr();
        Fnr aktivFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(historiskFnr.get(), true, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktivFnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));

        UUID huskelappPaaAktivFnr = UUID.randomUUID();
        UUID huskelappPaaHistoriskFnr = UUID.randomUUID();
        insertAktivHuskelapp(huskelappPaaAktivFnr, aktivFnr, ZonedDateTime.now().minusDays(2));
        insertAktivHuskelapp(huskelappPaaHistoriskFnr, historiskFnr, ZonedDateTime.now());

        assertThat(huskelappService.hentHuskelapp(aktivFnr).map(Huskelapp::huskelappId)).contains(huskelappPaaAktivFnr);
    }

    @Test
    public void skalSletteHuskelapperPaaAlleIdenterForBruker() {
        Fnr historiskFnr = randomFnr();
        Fnr aktivFnr = randomFnr();
        Fnr annenBrukerFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(historiskFnr.get(), true, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktivFnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));

        insertAktivHuskelapp(UUID.randomUUID(), historiskFnr, ZonedDateTime.now().minusDays(1));
        insertAktivHuskelapp(UUID.randomUUID(), aktivFnr, ZonedDateTime.now());
        UUID annenBrukersHuskelapp = UUID.randomUUID();
        insertAktivHuskelapp(annenBrukersHuskelapp, annenBrukerFnr, ZonedDateTime.now());

        huskelappService.sletteAlleHuskelapperPaaBruker(aktorId, Optional.of(aktivFnr));

        assertThat(jdbcTemplate.queryForList("SELECT HUSKELAPP_ID FROM HUSKELAPP", UUID.class)).containsExactly(annenBrukersHuskelapp);
    }

    @Test
    public void brukerHarHuskelappPaForrigeNavkontorSkalSjekkeHuskelappPaaHistoriskFnr() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        insertOppfolgingsInformasjon(aktivFnr, aktorId, VeilederId.of("1111"), EnhetId.of("0110"), UUID.randomUUID());
        leggTilHistoriskFnr(aktivFnr, historiskFnr, aktorId);
        insertAktivHuskelapp(UUID.randomUUID(), historiskFnr, "0220", ZonedDateTime.now());

        assertThat(huskelappService.brukerHarHuskelappPaForrigeNavkontor(aktorId, Optional.of(aktivFnr))).isTrue();
    }

    @Test
    public void brukerHarHuskelappPaForrigeNavkontorSkalPrioritereHuskelappPaaAktivFnr() {
        Fnr aktivFnr = randomFnr();
        Fnr historiskFnr = randomFnr();
        AktorId aktorId = randomAktorId();
        insertOppfolgingsInformasjon(aktivFnr, aktorId, VeilederId.of("1111"), EnhetId.of("0110"), UUID.randomUUID());
        leggTilHistoriskFnr(aktivFnr, historiskFnr, aktorId);
        insertAktivHuskelapp(UUID.randomUUID(), aktivFnr, "0110", ZonedDateTime.now().minusDays(2));
        insertAktivHuskelapp(UUID.randomUUID(), historiskFnr, "0220", ZonedDateTime.now());

        assertThat(huskelappService.brukerHarHuskelappPaForrigeNavkontor(aktorId, Optional.of(aktivFnr))).isFalse();
    }

    private void leggTilHistoriskFnr(Fnr aktivFnr, Fnr historiskFnr, AktorId aktorId) {
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(aktivFnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(historiskFnr.get(), true, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));
    }

    private void insertAktivHuskelapp(UUID huskelappId, Fnr fnr, ZonedDateTime endretDato) {
        insertAktivHuskelapp(huskelappId, fnr, "0110", endretDato);
    }

    private void insertAktivHuskelapp(UUID huskelappId, Fnr fnr, String enhetId, ZonedDateTime endretDato) {
        jdbcTemplate.update("INSERT INTO HUSKELAPP (HUSKELAPP_ID, FNR, ENHET_ID, ENDRET_AV_VEILEDER, ENDRET_DATO, FRIST, KOMMENTAR, STATUS) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                huskelappId, fnr.get(), enhetId, "1111", Timestamp.from(endretDato.toInstant()), null, "kommentar", "AKTIV");
    }

    @Test
    public void testHentNavKontorForHistoriskFnrKasterIkkeNullPointerException() {
        // Regresjonstest for NPE i BrukerServiceV2.hentNavKontor(Fnr): et fnr uten aktiv
        // fnr<->aktørId-mapping i PDL (f.eks. et historisk fødselsnummer) skal gi
        // Optional.empty(), ikke kaste NullPointerException. Kaller vi bevisst ikke
        // insertOppfolgingsInformasjon(...) for dette fnr-et, slik at verken bruker_identer
        // eller ao_kontor har noen rad for det - akkurat situasjonen som trigget NPE-en i
        // produksjon (se HuskelappController.hentErBrukerUfordelt).
        Fnr historiskFnr = randomFnr();

        Optional<NavKontor> navKontor = brukerServiceV2.hentNavKontor(historiskFnr);

        assertThat(navKontor).isEmpty();
    }

    private void insertOppfolgingsInformasjon(Fnr fnr, AktorId aktorId, VeilederId veilederId, EnhetId navKontor, UUID oppfolgingsperiodeId) {
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now(), oppfolgingsperiodeId);
        jdbcTemplate.update("INSERT INTO oppfolgingsbruker_arena_v2 (fodselsnr, nav_kontor) values (?,?)", fnr.get(), navKontor.get());
        jdbcTemplate.update("INSERT INTO ao_kontor (ident, kontor_id, aktorid) VALUES (?,?,?)", fnr.get(), navKontor.get(), aktorId.get());
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now(), oppfolgingsperiodeId);
        oppfolgingRepositoryV2.settVeileder(aktorId, veilederId);
    }
}
