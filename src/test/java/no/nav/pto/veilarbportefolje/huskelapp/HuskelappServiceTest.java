package no.nav.pto.veilarbportefolje.huskelapp;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
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

    public HuskelappServiceTest() {
        jdbcTemplate = SingletonPostgresContainer.init().createJdbcTemplate();
        this.pdlIdentRepository = new PdlIdentRepository(jdbcTemplate);
        OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(jdbcTemplate, null);
        this.oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(jdbcTemplate);
        BrukerServiceV2 brukerServiceV2 = new BrukerServiceV2(new PdlIdentRepository(jdbcTemplate), oppfolgingsbrukerRepositoryV3, oppfolgingRepositoryV2);
        huskelappService = new HuskelappService(Mockito.mock(OpensearchIndexerV2.class), brukerServiceV2, new HuskelappRepository(jdbcTemplate, jdbcTemplate));
    }

    @BeforeEach
    public void setUp() {
        this.jdbcTemplate.execute("TRUNCATE TABLE HUSKELAPP");
        this.jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        this.jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        this.jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }

    @Test
    public void testRedigereOgHenteHuskelapp() {
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        EnhetId enhetId = EnhetId.of("0110");
        VeilederId veilederId1 = VeilederId.of("1111");

        insertOppfolgingsInformasjon(fnr1, aktorId1, veilederId1, enhetId);
        insertOppfolgingsInformasjon(fnr2, aktorId2, veilederId1, enhetId);

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
        EnhetId enhetId = EnhetId.of("0110");
        VeilederId veilederId1 = VeilederId.of("1111");
        VeilederId veilederId2 = VeilederId.of("2222");

        insertOppfolgingsInformasjon(fnr1, aktorId1, veilederId1, enhetId);
        insertOppfolgingsInformasjon(fnr2, aktorId2, veilederId2, enhetId);

        HuskelappOpprettRequest huskelapp1 = new HuskelappOpprettRequest(fnr1, LocalDate.of(2024, 2, 10), "Husk nr 1");
        UUID huskelappUUID = huskelappService.opprettHuskelapp(huskelapp1, veilederId1);
        HuskelappRedigerRequest huskelapp2 = new HuskelappRedigerRequest(huskelappUUID, fnr1, LocalDate.of(2026, 1, 1), "Husk nr 2");
        huskelappService.redigerHuskelapp(huskelapp2, veilederId2);

        Optional<Huskelapp> result1 = huskelappService.hentHuskelapp(fnr1);
        assertThat(result1.isPresent()).isTrue();
    }

    @Test
    public void annenVeilederSkalKunneRedigereHuskelapp() {
        Fnr fnr1 = randomFnr();
        Fnr fnr2 = randomFnr();
        AktorId aktorId1 = randomAktorId();
        AktorId aktorId2 = randomAktorId();
        EnhetId enhetId = EnhetId.of("0110");
        VeilederId veilederId1 = VeilederId.of("1111");
        VeilederId veilederId2 = VeilederId.of("2222");

        insertOppfolgingsInformasjon(fnr1, aktorId1, veilederId1, enhetId);
        insertOppfolgingsInformasjon(fnr2, aktorId2, veilederId2, enhetId);

        HuskelappOpprettRequest huskelapp3 = new HuskelappOpprettRequest(fnr1,
                LocalDate.of(2026, 10, 11), ("Huskelapp nr.3 sin kommentar"));

        LocalDate nyFrist = LocalDate.of(2025, 10, 11);
        huskelappService.opprettHuskelapp(huskelapp3, veilederId1);
        Optional<Huskelapp> huskelappOriginal = huskelappService.hentHuskelapp(huskelapp3.brukerFnr());
        assertThat(huskelappOriginal.isPresent()).isTrue();
        assertThat(huskelappOriginal.get().kommentar()).isEqualTo("Huskelapp nr.3 sin kommentar");
        assertThat(huskelappOriginal.get().endretAv()).isEqualTo(veilederId1);

        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappOriginal.get().huskelappId(), huskelapp3.brukerFnr(), nyFrist, "ny kommentar på huskelapp nr.3");
        huskelappService.redigerHuskelapp(huskelappRedigerRequest, veilederId2);

        Optional<Huskelapp> huskelappOppdatertAvNyVeileder = huskelappService.hentHuskelapp(huskelapp3.brukerFnr());
        assertThat(huskelappOppdatertAvNyVeileder.isPresent()).isTrue();
        assertThat(huskelappOppdatertAvNyVeileder.get().kommentar()).isEqualTo("ny kommentar på huskelapp nr.3");
        assertThat(huskelappOppdatertAvNyVeileder.get().endretAv()).isEqualTo(veilederId2);
        assertThat(huskelappOppdatertAvNyVeileder.get().frist()).isEqualTo(nyFrist);
    }

    private void insertOppfolgingsInformasjon(Fnr fnr, AktorId aktorId, VeilederId veilederId, EnhetId navKontor) {
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT),
                new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID)));
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        jdbcTemplate.update("INSERT INTO oppfolgingsbruker_arena_v2 (fodselsnr, nav_kontor) values (?,?)", fnr.get(), navKontor.get());
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        oppfolgingRepositoryV2.settVeileder(aktorId, veilederId);
    }
}