package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class OppfolginsbrukerRepositoryTestV2 {
    private JdbcTemplate db;
    private OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2;
    private final AktorId aktoerId = AktorId.of("0");

    @Autowired
    public void OppfolginsbrukerRepositoryTestV2(@Qualifier("PostgresJdbc") JdbcTemplate db, OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2) {
        this.db = db;
        this.oppfolgingsbrukerRepositoryV2 = oppfolgingsbrukerRepositoryV2;
    }

    @BeforeEach
    public void resetDb() {
        db.execute("truncate oppfolgingsbruker_arena cascade ");
    }

    @Test
    public void skal_ikke_lagre_oppfolgingsbruker_med_eldre_endret_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1),
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, true, ZonedDateTime.now(), ZonedDateTime.now());
        OppfolgingsbrukerEntity old_msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1),
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, false, null, ZonedDateTime.now().minusDays(5));

        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolgingsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(msg);

        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(old_msg);
        assertThat(oppfolgingsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isNotEqualTo(old_msg);
    }


    @Test
    public void skal_oppdater_oppfolgingsbruker_fra_nyere_dato() {
        OppfolgingsbrukerEntity msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1), "" +
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", true, true, false, null, ZonedDateTime.now().minusDays(5));
        OppfolgingsbrukerEntity new_msg = new OppfolgingsbrukerEntity(aktoerId.get(), "12015678912", "TEST", ZonedDateTime.now().minusDays(1), "" +
                "Tester", "Testerson", "1001", "ORG", "OP", "TES", "IKKE",
                "1234", false, true, true, ZonedDateTime.now(), ZonedDateTime.now());

        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(msg);
        assertThat(oppfolgingsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(msg);

        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(new_msg);
        assertThat(oppfolgingsbrukerRepositoryV2.getOppfolgingsBruker(aktoerId).get()).isEqualTo(new_msg);
    }


    @Test
    public void skjerming_sperretAnsatt() {
        AktorId sperretAnsatt = randomAktorId();
        AktorId kontroll = randomAktorId();
        String sperretAnsattFnr = randomFnr().get();
        String kontrollFnr = randomFnr().get();
        settSperretAnsatt(sperretAnsatt, sperretAnsattFnr, true);
        settSperretAnsatt(kontroll, kontrollFnr, false);

        List<String> medTilgang = oppfolgingsbrukerRepositoryV2.hentSkjermedeBrukere(List.of(sperretAnsattFnr, kontrollFnr),
                false, false, true);
        List<String> utenTilgang = oppfolgingsbrukerRepositoryV2.hentSkjermedeBrukere(List.of(sperretAnsattFnr, kontrollFnr),
                false, false, false);
        assertThat(medTilgang.size()).isEqualTo(0);
        assertThat(utenTilgang.size()).isEqualTo(1);
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(sperretAnsattFnr))).isTrue();
    }

    @Test
    public void skjerming_diskresjonskode() {
        AktorId kode6 = randomAktorId();
        AktorId kode7 = randomAktorId();
        AktorId kontroll = randomAktorId();
        String kode6Fnr = randomFnr().get();
        String kode7Fnr = randomFnr().get();
        String kontrollFnr = randomFnr().get();
        settDiskresjonskode(kode6, kode6Fnr, "6");
        settDiskresjonskode(kode7, kode7Fnr, "7");
        settDiskresjonskode(kontroll, kontrollFnr, null);

        List<String> medAlleTilgang = oppfolgingsbrukerRepositoryV2.hentSkjermedeBrukere(List.of(kode6Fnr, kode7Fnr,kontrollFnr),
                true, true, false);
        List<String> medKode6Tilgang = oppfolgingsbrukerRepositoryV2.hentSkjermedeBrukere(List.of(kode6Fnr, kode7Fnr,kontrollFnr),
                true, false, false);
        List<String> medKode7Tilgang = oppfolgingsbrukerRepositoryV2.hentSkjermedeBrukere(List.of(kode6Fnr, kode7Fnr,kontrollFnr),
                false, true, false);
        List<String> utenTilgang = oppfolgingsbrukerRepositoryV2.hentSkjermedeBrukere(List.of(kode6Fnr, kode7Fnr,kontrollFnr),
                false, false, false);

        assertThat(medAlleTilgang.size()).isEqualTo(0);
        assertThat(medKode6Tilgang.size()).isEqualTo(1);
        assertThat(medKode7Tilgang.size()).isEqualTo(1);
        assertThat(utenTilgang.size()).isEqualTo(2);

        assertThat(medKode6Tilgang.stream().anyMatch(x -> x.equals(kode7Fnr))).isTrue();
        assertThat(medKode7Tilgang.stream().anyMatch(x -> x.equals(kode6Fnr))).isTrue();
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(kode6Fnr))).isTrue();
        assertThat(utenTilgang.stream().anyMatch(x -> x.equals(kode7Fnr))).isTrue();
    }

    private void settSperretAnsatt(AktorId aktoer, String fnr, boolean sperret){
        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktoer.get(), fnr, null, null,
                        null, null, "0000", null, null,
                        null, null, null, true, sperret,
                        false, null, ZonedDateTime.now()));
    }

    private void settDiskresjonskode(AktorId aktoer, String fnr, String kode){
        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktoer.get(), fnr, null, null,
                        null, null, "0000", null, null,
                        null, null, kode, true, false,
                        false, null, ZonedDateTime.now()));
    }
}
