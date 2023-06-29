package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.oppfolging.SkjermingRepository;
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
public class OppfolginsbrukerRepositoryV3Test {
    private JdbcTemplate db;
    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;

    private SkjermingRepository skjermingRepository;


    private final Fnr fnr = Fnr.of("0");

    @Autowired
    public void OppfolginsbrukerRepositoryTestV2(JdbcTemplate db, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository) {
        this.db = db;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        skjermingRepository = new SkjermingRepository(db);
    }

    @BeforeEach
    public void resetDb() {
        db.execute("truncate oppfolgingsbruker_arena_v2");
        db.update("truncate bruker_identer");
        db.update("truncate nom_skjerming");

    }

    @Test
    public void skal_ikke_lagre_oppfolgingsbruker_med_eldre_endret_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1),
                "1001", "ORG", "OP", "TES", ZonedDateTime.now());
        OppfolgingsbrukerEntity old_msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1),
                "1002", "ORG", "OP", "TES", ZonedDateTime.now().minusDays(5));

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolgingsbrukerRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(msg);

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(old_msg);
        assertThat(oppfolgingsbrukerRepository.getOppfolgingsBruker(fnr).get()).isNotEqualTo(old_msg);
    }


    @Test
    public void skal_oppdater_oppfolgingsbruker_fra_nyere_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(fnr.get(),"TEST", ZonedDateTime.now().minusDays(1),
                "1001", "ORG", "OP", "TES", ZonedDateTime.now().minusDays(5));
        OppfolgingsbrukerEntity new_msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", ZonedDateTime.now().minusDays(1), "1001", "ORG", "OP", "TES", ZonedDateTime.now());

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

        List<String> medTilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(sperretAnsattFnr, kontrollFnr), new BrukerinnsynTilganger(
                false, false, true));
        List<String> utenTilgang = oppfolgingsbrukerRepository.finnSkjulteBrukere(List.of(sperretAnsattFnr, kontrollFnr),
                new BrukerinnsynTilganger(false, false, false));
        assertThat(medTilgang.size()).isEqualTo(0);
        assertThat(utenTilgang.size()).isEqualTo(1);
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(sperretAnsattFnr))).isTrue();
    }

    private void settSperretAnsatt(String fnr, boolean sperret) {
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr, null, null, "0000", null, null,
                        null,
                         ZonedDateTime.now()));
        skjermingRepository.settSkjerming(Fnr.of(fnr), sperret);
    }


}
