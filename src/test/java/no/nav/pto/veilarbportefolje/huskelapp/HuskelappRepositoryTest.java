package no.nav.pto.veilarbportefolje.huskelapp;

import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappOpprettRequest;
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.HuskelappRedigerRequest;
import no.nav.pto.veilarbportefolje.huskelapp.domain.Huskelapp;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class HuskelappRepositoryTest {
    @Autowired
    private HuskelappRepository repo;
    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;


    Fnr fnr1 = Fnr.ofValidFnr("01010101010");
    Fnr fnr2 = Fnr.ofValidFnr("01010101015");
    Fnr fnr3 = Fnr.ofValidFnr("01010101012");
    Fnr fnr4 = Fnr.ofValidFnr("01010101013");
    LocalDate frist1 = LocalDate.of(2026, 1, 1);

    EnhetId enhet0010 = EnhetId.of("0010");
    EnhetId enhet2420 = EnhetId.of("2420");

    VeilederId veilederA = VeilederId.of("Z123456");
    VeilederId veilederB = VeilederId.of("Z987654");

    private final HuskelappOpprettRequest huskelapp1 = new HuskelappOpprettRequest(fnr1,
            frist1, ("Huskelapp nr.1 sin kommentar"));

    private final HuskelappOpprettRequest huskelapp2 = new HuskelappOpprettRequest(fnr2,
            LocalDate.of(2017, 2, 27), ("Huskelapp nr.2 sin kommentar"));

    private final HuskelappOpprettRequest huskelapp3 = new HuskelappOpprettRequest(fnr3,
            LocalDate.of(2026, 10, 11), ("Huskelapp nr.3 sin kommentar"));

    private final HuskelappOpprettRequest huskelappUtenKommentar = new HuskelappOpprettRequest(fnr4,
            LocalDate.of(2030, 1, 1), (null));

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE HUSKELAPP");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolging_data");
        jdbcTemplate.execute("TRUNCATE TABLE oppfolgingsbruker_arena_v2");
        jdbcTemplate.execute("TRUNCATE TABLE bruker_identer");
    }


    @Test
    public void skalKunneOppretteOgHenteHuskelapp() {
        repo.opprettHuskelapp(huskelapp1, veilederA, enhet0010);
        Optional<Huskelapp> result = repo.hentAktivHuskelapp(fnr1);
        assertThat(result.isPresent()).isTrue();
        Optional<Huskelapp> result2 = repo.hentAktivHuskelapp(result.get().huskelappId());
        assertThat(result2.isPresent()).isTrue();
        assertThat(result.get().enhetId().toString()).isEqualTo("0010").isEqualTo(result2.get().enhetId().toString());
        assertThat(result.get().frist()).isEqualTo(frist1).isEqualTo(result2.get().frist());
    }

    @Test
    public void skalKunneOppretteOgRedigereOgHenteHuskelappUtenKommentar() {
        repo.opprettHuskelapp(huskelappUtenKommentar, veilederA, enhet0010);
        Optional<Huskelapp> huskelappUtenKommentar_ = repo.hentAktivHuskelapp(huskelappUtenKommentar.brukerFnr());
        assertThat(huskelappUtenKommentar_.isPresent()).isTrue();
        assertThat(huskelappUtenKommentar_.get().kommentar()).isEqualTo(null);
        LocalDate nyFrist = LocalDate.of(2025, 10, 11);
        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappUtenKommentar_.get().huskelappId(), huskelappUtenKommentar.brukerFnr(), nyFrist, null);
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederB, enhet0010);
        Optional<Huskelapp> oppdatertHuskelappUtenKommentar = repo.hentAktivHuskelapp(fnr4);
        assertThat(oppdatertHuskelappUtenKommentar.isPresent()).isTrue();
        assertThat(oppdatertHuskelappUtenKommentar.get().kommentar()).isEqualTo(null);
    }


    @Test
    public void annenVeilederSkalKunneRedigereHuskelapp() {
        LocalDate nyFrist = LocalDate.of(2025, 10, 11);
        repo.opprettHuskelapp(huskelapp3, veilederA, enhet2420);
        Optional<Huskelapp> huskelappOriginal = repo.hentAktivHuskelapp(huskelapp3.brukerFnr());
        assertThat(huskelappOriginal.isPresent()).isTrue();
        assertThat(huskelappOriginal.get().kommentar()).isEqualTo("Huskelapp nr.3 sin kommentar");
        assertThat(huskelappOriginal.get().endretAv()).isEqualTo(veilederA);
        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappOriginal.get().huskelappId(), huskelapp3.brukerFnr(), nyFrist, "ny kommentar på huskelapp nr.3");
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederB, enhet0010);
        Optional<Huskelapp> huskelappOppdatertAvNyVeileder = repo.hentAktivHuskelapp(huskelapp3.brukerFnr());
        assertThat(huskelappOppdatertAvNyVeileder.isPresent()).isTrue();
        assertThat(huskelappOppdatertAvNyVeileder.get().kommentar()).isEqualTo("ny kommentar på huskelapp nr.3");
        assertThat(huskelappOppdatertAvNyVeileder.get().endretAv()).isEqualTo(veilederB);
        assertThat(huskelappOppdatertAvNyVeileder.get().frist()).isEqualTo(nyFrist);
    }

    @Test
    public void skalKunneSletteHuskelapp() {
        repo.opprettHuskelapp(huskelapp2, veilederA, enhet0010);
        Optional<Huskelapp> huskelapp = repo.hentAktivHuskelapp(fnr2);
        assertThat(huskelapp.isPresent()).isTrue();
        assertThat(huskelapp.get().kommentar()).isEqualTo("Huskelapp nr.2 sin kommentar");
        repo.settSisteHuskelappRadIkkeAktiv(huskelapp.get().huskelappId());
        Optional<Huskelapp> huskelappEtter = repo.hentAktivHuskelapp(fnr2);
        assertThat(huskelappEtter.isPresent()).isFalse();
    }

    @Test
    public void skalKunneInaktivereNyesteHuskelappRadNaarFlereRader() {
        repo.opprettHuskelapp(huskelapp2, veilederA, enhet0010);
        Optional<Huskelapp> huskelappFoer = repo.hentAktivHuskelapp(fnr2);
        assertThat(huskelappFoer.isPresent()).isTrue();
        assertThat(huskelappFoer.get().kommentar()).isEqualTo("Huskelapp nr.2 sin kommentar");
        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappFoer.get().huskelappId(), fnr2, huskelappFoer.get().frist(), "ny kommentar på huskelapp nr.2");
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010);
        List<Huskelapp> alleHuskelappRader = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId());
        assertThat(alleHuskelappRader.size()).isEqualTo(2);
        repo.settSisteHuskelappRadIkkeAktiv(huskelappFoer.get().huskelappId());
        Optional<Huskelapp> huskelappEtter = repo.hentAktivHuskelapp(fnr2);
        assertThat(huskelappEtter.isPresent()).isFalse();
    }


    @Test
    public void sletterAlleHuskelappRader() {
        repo.opprettHuskelapp(huskelapp2, veilederA, enhet0010);
        Optional<Huskelapp> huskelappFoer = repo.hentAktivHuskelapp(huskelapp2.brukerFnr());
        assertThat(huskelappFoer.isPresent()).isTrue();
        assertThat(huskelappFoer.get().kommentar()).isEqualTo("Huskelapp nr.2 sin kommentar");

        HuskelappRedigerRequest huskelappRedigerRequest = new HuskelappRedigerRequest(huskelappFoer.get().huskelappId(), huskelapp2.brukerFnr(), huskelappFoer.get().frist(), "ny kommentar på huskelapp nr.2");
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010);
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010);
        repo.redigerHuskelapp(huskelappRedigerRequest, veilederA, enhet0010);

        List<Huskelapp> alleHuskelappRader = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId());
        assertThat(alleHuskelappRader.size()).isEqualTo(4);

        repo.slettAlleHuskelappRaderPaaBruker(huskelapp2.brukerFnr());

        List<Huskelapp> alleHuskelappRader2 = repo.hentAlleRaderPaHuskelapp(huskelappFoer.get().huskelappId());
        assertThat(alleHuskelappRader2.size()).isEqualTo(0);
    }


    @Test
    public void faarHentetNavkontorPaHuskelapp() {
        repo.opprettHuskelapp(huskelapp2, veilederA, enhet0010);
        Optional<String> enhetId = repo.hentNavkontorPaHuskelapp(huskelapp2.brukerFnr());
        assertThat(enhetId.isPresent()).isTrue();
    }
}
