package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.I_AKTIVITET;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AktiviteterOpensearchIntegrasjon extends EndToEndTest {
    private final AktivitetService aktivitetService;
    private final OpensearchService opensearchService;
    private final OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2;
    private final AktorId aktoer = randomAktorId();
    private final Fnr fodselsnummer = Fnr.ofValidFnr("10108000399"); //TESTFAMILIE
    private final JdbcTemplate jdbcTemplatePostgres;
    private final Motedeltaker skjermetDeltaker = new Motedeltaker("", "", null);

    @Autowired
    public AktiviteterOpensearchIntegrasjon(AktivitetService aktivitetService, OpensearchService opensearchService, OppfolgingsbrukerRepositoryV2 oppfolgingsbrukerRepositoryV2, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplatePostgres) {
        this.aktivitetService = aktivitetService;
        this.opensearchService = opensearchService;
        this.oppfolgingsbrukerRepositoryV2 = oppfolgingsbrukerRepositoryV2;
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
    }

    @BeforeEach
    public void resetDb() {
        jdbcTemplatePostgres.update("TRUNCATE aktiviteter CASCADE");
        jdbcTemplatePostgres.update("TRUNCATE oppfolgingsbruker_arena CASCADE");
        jdbcTemplatePostgres.update("TRUNCATE oppfolging_data CASCADE");
    }

    @Test
    public void lasteroppeikkelagreteaktiviteteter() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.setupBruker(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of(I_AKTIVITET)),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                }
        );
    }

    @Test
    public void hentMoteplan() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();
        VeilederId annenVeileder = randomVeilederId();
        testDataClient.setupBruker(aktoer, navKontor, veileder, ZonedDateTime.now());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now().plusDays(2))
                .setTilDato(ZonedDateTime.now().plusDays(2))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(true));
        // Møte satt tilbake i tid
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("3")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now().minusDays(2))
                .setTilDato(ZonedDateTime.now().minusDays(2))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(true));
        List<Moteplan> moteplaner = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));
        List<Moteplan> ingenMotePlaner = aktivitetService.hentMoteplan(annenVeileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));

        assertThat(moteplaner.size()).isEqualTo(2);
        assertThat(ingenMotePlaner.size()).isEqualTo(0);
    }

    @Test
    public void hentMoteplan_sperretAnsatt() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();

        testDataClient.setupBruker(aktoer, navKontor, veileder, ZonedDateTime.now());
        settSperretAnsatt(aktoer, randomFnr(), navKontor);

        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        List<Moteplan> medTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, true));
        List<Moteplan> utenTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));

        assertThat(medTilgang.stream().noneMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
        assertThat(utenTilgang.stream().allMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
    }


    @Test
    public void hentMoteplan_diskresjonsKode() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();

        AktorId aktoerKode6 = randomAktorId();
        AktorId aktoerKode7 = randomAktorId();

        testDataClient.setupBruker(aktoerKode6, navKontor, veileder, ZonedDateTime.now());
        testDataClient.setupBruker(aktoerKode7, navKontor, veileder, ZonedDateTime.now());
        settDiskresjonskode(aktoerKode6, randomFnr(), navKontor, "6");
        settDiskresjonskode(aktoerKode7, randomFnr(), navKontor, "7");

        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("1")
                .setAktorId(aktoerKode6.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoerKode7.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.now())
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false));
        List<Moteplan> medTilgang_alt = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(true, true, false));
        List<Moteplan> medTilgang_6 = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(true, false, false));
        List<Moteplan> medTilgang_7 = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, true, false));
        List<Moteplan> utenTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new Skjermettilgang(false, false, false));

        assertThat(medTilgang_alt.stream().noneMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
        assertThat(medTilgang_6.stream().filter(moteplan -> moteplan.deltaker().equals(skjermetDeltaker)).toList().size()).isEqualTo(1);
        assertThat(medTilgang_7.stream().filter(moteplan -> moteplan.deltaker().equals(skjermetDeltaker)).toList().size()).isEqualTo(1);
        assertThat(utenTilgang.stream().allMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
    }

    private void settSperretAnsatt(AktorId aktoer, Fnr fnr, NavKontor navKontor) {
        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktoer.get(), fnr.get(), null, null,
                        "test", "testson", navKontor.getValue(), null, null,
                        null, null, null, true, true,
                        false, null, ZonedDateTime.now()));
    }

    private void settDiskresjonskode(AktorId aktoer, Fnr fnr, NavKontor navKontor, String kode) {
        oppfolgingsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(aktoer.get(), fnr.get(), null, null,
                        "test", "testson", navKontor.getValue(), null, null,
                        null, null, kode, true, false,
                        false, null, ZonedDateTime.now()));
    }
}
