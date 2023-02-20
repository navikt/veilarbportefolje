package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerInnsynTilganger;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class OppfolginsbrukerRepositoryTestV3 {
    private JdbcTemplate db;
    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final Fnr fnr = Fnr.of("0");

    @Autowired
    public void OppfolginsbrukerRepositoryTestV2( JdbcTemplate db, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository) {
        this.db = db;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
    }

    @BeforeEach
    public void resetDb() {
        db.execute("truncate oppfolgingsbruker_arena_v2");
        db.update("truncate bruker_identer");
    }

    @Test
    public void skal_ikke_lagre_oppfolgingsbruker_med_eldre_endret_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1),
                "Tester_new", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, ZonedDateTime.now());
        OppfolgingsbrukerEntity old_msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1),
                "Tester_old", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, false, ZonedDateTime.now().minusDays(5));

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolgingsbrukerRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(msg);

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(old_msg);
        assertThat(oppfolgingsbrukerRepository.getOppfolgingsBruker(fnr).get()).isNotEqualTo(old_msg);
    }


    @Test
    public void skal_oppdater_oppfolgingsbruker_fra_nyere_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1), "" +
                "Tester_old", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, false, ZonedDateTime.now().minusDays(5));
        OppfolgingsbrukerEntity new_msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1), "" +
                "Tester_new", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, ZonedDateTime.now());

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolgingsbrukerRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(msg);

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(new_msg);
        assertThat(oppfolgingsbrukerRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(new_msg);
    }


    @Test
    public void skjerming_sperretAnsatt() {
        String sperretAnsattFnr = randomFnr().get();
        String kontrollFnr = randomFnr().get();
        settSperretAnsatt(sperretAnsattFnr, true);
        settSperretAnsatt(kontrollFnr, false);

        List<String> medTilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(sperretAnsattFnr, kontrollFnr), new BrukerInnsynTilganger(
                false, false, true));
        List<String> utenTilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(sperretAnsattFnr, kontrollFnr),
                new BrukerInnsynTilganger(false, false, false));
        assertThat(medTilgang.size()).isEqualTo(0);
        assertThat(utenTilgang.size()).isEqualTo(1);
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(sperretAnsattFnr))).isTrue();
    }

    @Test
    public void skjerming_diskresjonskode() {
        String kode6Fnr = randomFnr().get();
        String kode7Fnr = randomFnr().get();
        String kontrollFnr = randomFnr().get();

        settDiskresjonskode(kode6Fnr, "6");
        settDiskresjonskode(kode7Fnr, "7");
        settDiskresjonskode(kontrollFnr, null);

        List<String> medAlleTilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(kode6Fnr, kode7Fnr, kontrollFnr),
                new BrukerInnsynTilganger(true, true, false));
        List<String> medKode6Tilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(kode6Fnr, kode7Fnr, kontrollFnr),
                new BrukerInnsynTilganger(true, false, false));
        List<String> medKode7Tilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(kode6Fnr, kode7Fnr, kontrollFnr),
                new BrukerInnsynTilganger(false, true, false));
        List<String> utenTilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(kode6Fnr, kode7Fnr, kontrollFnr),
                new BrukerInnsynTilganger(false, false, false));

        assertThat(medAlleTilgang.size()).isEqualTo(0);
        assertThat(medKode6Tilgang.size()).isEqualTo(1);
        assertThat(medKode7Tilgang.size()).isEqualTo(1);
        assertThat(utenTilgang.size()).isEqualTo(2);

        assertThat(medKode6Tilgang.stream().anyMatch(x -> x.equals(kode7Fnr))).isTrue();
        assertThat(medKode7Tilgang.stream().anyMatch(x -> x.equals(kode6Fnr))).isTrue();
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(kode6Fnr))).isTrue();
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(kode7Fnr))).isTrue();
    }

    private void settSperretAnsatt(String fnr, boolean sperret) {
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr, null, null,
                        null, null, "0000", null, null,
                        null, null, null, sperret,
                        false, ZonedDateTime.now()));
    }

    private void settDiskresjonskode(String fnr, String kode) {
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr, null, null,
                        null, null, "0000", null, null,
                        null, null, kode, false,
                        false, ZonedDateTime.now()));
    }
}
