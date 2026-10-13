package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.NavKontor;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.skjerming.SkjermingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.pto.veilarbportefolje.util.DateUtils.now;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class OppfolginsbrukerRepositoryV3Test {
    private JdbcTemplate db;
    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private OppfolgingsbrukerTestRepository oppfolgingsbrukerTestRepository;

    private SkjermingRepository skjermingRepository;

    private final Fnr fnr = Fnr.of("01010112345");
    private final AktorId aktorId = AktorId.of("1111111111111");

    @Autowired
    private PdlIdentRepository pdlIdentRepository;

    @Autowired
    public void OppfolginsbrukerRepositoryTestV2(JdbcTemplate db, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, OppfolgingsbrukerTestRepository oppfolgingsbrukerTestRepository) {
        this.db = db;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        this.oppfolgingsbrukerTestRepository = oppfolgingsbrukerTestRepository;
        skjermingRepository = new SkjermingRepository(db);
    }

    @BeforeEach
    public void resetDb() {
        db.execute("truncate oppfolgingsbruker_arena_v2");
        db.update("truncate bruker_identer");
        db.update("truncate nom_skjerming");
        db.update("truncate ao_kontor");
    }

    @Test
    public void skal_ikke_lagre_oppfolgingsbruker_med_eldre_endret_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", now().minusDays(1),
                "1001", "ORG", "OP", "TES", now());
        OppfolgingsbrukerEntity old_msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", now().minusDays(1),
                "1002", "ORG", "OP", "TES", now().minusDays(5));
        pdlIdentRepository.upsertIdenter(
                List.of(
                        new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID),
                        new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT)
                )
        );

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(msg, new NavKontor(msg.nav_kontor()), aktorId);
        assertThat(oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(msg);

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(old_msg,  new NavKontor(old_msg.nav_kontor()), aktorId);
        assertThat(oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr).get()).isNotEqualTo(old_msg);
    }


    @Test
    public void skal_oppdater_oppfolgingsbruker_fra_nyere_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(fnr.get(),"TEST", now().minusDays(1),
                "1001", "ORG", "OP", "TES", now().minusDays(5));
        OppfolgingsbrukerEntity new_msg = new OppfolgingsbrukerEntity(fnr.get(), "TEST", now().minusDays(1), "1001", "ORG", "OP", "TES", now());
        pdlIdentRepository.upsertIdenter(
                List.of(
                        new PDLIdent(aktorId.get(), false, PDLIdent.Gruppe.AKTORID),
                        new PDLIdent(fnr.get(), false, PDLIdent.Gruppe.FOLKEREGISTERIDENT)
                )
        );

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(msg, new NavKontor(msg.nav_kontor()), aktorId);
        assertThat(oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(msg);

        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(new_msg, new NavKontor(new_msg.nav_kontor()), aktorId);
        assertThat(oppfolgingsbrukerTestRepository.getOppfolgingsBruker(fnr).get()).isEqualTo(new_msg);
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
                         now()),
                new NavKontor("0000"),
                aktorId);
        skjermingRepository.settSkjerming(Fnr.of(fnr), sperret);
    }

    @Test
    public void oppdaterIdentPaaNavKontor_skal_flytte_ident_til_nytt_fnr() {
        Fnr gammeltFnr = Fnr.of("11111111111");
        Fnr nyttFnr = Fnr.of("22222222222");
        oppfolgingsbrukerRepository.upsertNavKontor(aktorId, gammeltFnr, new NavKontor("1001"));

        oppfolgingsbrukerRepository.oppdaterIdentPaaNavKontor(aktorId, gammeltFnr, nyttFnr);

        assertThat(hentIdentPaaNavKontor(aktorId)).isEqualTo(nyttFnr.get());
        assertThat(oppfolgingsbrukerRepository.hentNavKontor(aktorId).get().getValue()).isEqualTo("1001");
    }

    @Test
    public void oppdaterIdentPaaNavKontor_skal_slette_utdatert_rad_med_nytt_fnr_paa_annen_aktorid() {
        Fnr gammeltFnr = Fnr.of("11111111111");
        Fnr nyttFnr = Fnr.of("22222222222");
        AktorId annenAktorId = AktorId.of("2222222222222");
        oppfolgingsbrukerRepository.upsertNavKontor(aktorId, gammeltFnr, new NavKontor("1001"));
        oppfolgingsbrukerRepository.upsertNavKontor(annenAktorId, nyttFnr, new NavKontor("2002"));

        oppfolgingsbrukerRepository.oppdaterIdentPaaNavKontor(aktorId, gammeltFnr, nyttFnr);

        assertThat(hentIdentPaaNavKontor(aktorId)).isEqualTo(nyttFnr.get());
        assertThat(oppfolgingsbrukerRepository.hentNavKontor(aktorId).get().getValue()).isEqualTo("1001");
        assertThat(oppfolgingsbrukerRepository.hentNavKontor(annenAktorId).isPresent()).isFalse();
    }

    private String hentIdentPaaNavKontor(AktorId aktorId) {
        return db.queryForObject("SELECT ident FROM ao_kontor WHERE aktorid = ?", String.class, aktorId.get());
    }
}
