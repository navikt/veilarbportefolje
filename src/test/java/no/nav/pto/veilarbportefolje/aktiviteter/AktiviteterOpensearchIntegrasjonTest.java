package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.oppfolging.SkjermingRepository;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.I_AKTIVITET;
import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AktiviteterOpensearchIntegrasjonTest extends EndToEndTest {
    private final AktivitetService aktivitetService;
    private final OpensearchService opensearchService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final AktorId aktoer = randomAktorId();
    private final Fnr fodselsnummer = Fnr.ofValidFnr("10108000399"); //TESTFAMILIE
    private final JdbcTemplate jdbcTemplatePostgres;
    private final PdlIdentRepository pdlIdentRepository;
    private final SkjermingRepository skjermingRepository;

    @Autowired
    public AktiviteterOpensearchIntegrasjonTest(AktivitetService aktivitetService, OpensearchService opensearchService, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, JdbcTemplate jdbcTemplatePostgres, PdlIdentRepository pdlIdentRepository) {
        this.aktivitetService = aktivitetService;
        this.opensearchService = opensearchService;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.pdlIdentRepository = pdlIdentRepository;
        skjermingRepository = new SkjermingRepository(jdbcTemplatePostgres);
    }

    @BeforeEach
    public void resetDb() {
        jdbcTemplatePostgres.update("TRUNCATE aktiviteter");
        jdbcTemplatePostgres.update("TRUNCATE oppfolgingsbruker_arena_v2");
        jdbcTemplatePostgres.update("TRUNCATE bruker_identer");
        jdbcTemplatePostgres.update("TRUNCATE oppfolging_data");
        jdbcTemplatePostgres.update("TRUNCATE nom_skjerming");
    }

    @Test
    public void lasteroppeikkelagreteaktiviteteter() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue(), null);
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
                            Sorteringsrekkefolge.STIGENDE,
                            Sorteringsfelt.IKKE_SATT,
                            new Filtervalg().setFerdigfilterListe(List.of(I_AKTIVITET)),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                }
        );
    }

    @Test
    public void lasteroppaktivitetStillingFraNAV() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue(), null);
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.STILLING_FRA_NAV)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(null)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false)
                .setStillingFraNavData(
                        new KafkaAktivitetMelding.StillingFraNAV()
                                .setCvKanDelesStatus(KafkaAktivitetMelding.CvKanDelesStatus.JA)
                                .setSvarfrist("2044-02-03T00:00:00+02:00"))
        );
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            Sorteringsrekkefolge.STIGENDE,
                            Sorteringsfelt.IKKE_SATT,
                            new Filtervalg().setNavnEllerFnrQuery(fodselsnummer.toString()).setFerdigfilterListe(new ArrayList<>()),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().getFirst().getNesteSvarfristCvStillingFraNav()).isEqualTo(LocalDate.parse("2044-02-03"));
                }
        );
    }

    @Test
    public void filtrerBrukerePaStillingFraNAV() {
        NavKontor navKontor = randomNavKontor();
        AktorId aktorIdCvDeltMedNav = randomAktorId();
        AktorId aktorIdIkkeDeltCv = randomAktorId();
        VeilederId veileder = randomVeilederId();
        testDataClient.lagreBrukerUnderOppfolging(aktorIdCvDeltMedNav, navKontor, veileder, ZonedDateTime.now(), null);
        testDataClient.lagreBrukerUnderOppfolging(aktorIdIkkeDeltCv, navKontor, veileder, ZonedDateTime.now(), null);
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktorIdCvDeltMedNav.toString())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.STILLING_FRA_NAV)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(null)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(false)
                .setStillingFraNavData(
                        new KafkaAktivitetMelding.StillingFraNAV()
                                .setCvKanDelesStatus(KafkaAktivitetMelding.CvKanDelesStatus.JA)
                                .setSvarfrist("2044-02-03T00:00:00+02:00"))
        );
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("4")
                .setAktorId(aktorIdIkkeDeltCv.toString())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(null)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setVersion(1L)
                .setAvtalt(true)
        );
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
            BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                    navKontor.getValue(),
                    empty(),
                    Sorteringsrekkefolge.STIGENDE,
                    Sorteringsfelt.IKKE_SATT,
                    new Filtervalg().setFerdigfilterListe(List.of(I_AKTIVITET)),
                    null,
                    null);

            assertThat(responseBrukere.getAntall()).isEqualTo(2);
        });


        BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                navKontor.getValue(),
                empty(),
                Sorteringsrekkefolge.STIGENDE,
                Sorteringsfelt.IKKE_SATT,
                new Filtervalg().setStillingFraNavFilter(List.of(StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA)).setFerdigfilterListe(new ArrayList<>()),
                null,
                null);

        System.out.println(JsonUtils.toJson(new Filtervalg().setStillingFraNavFilter(List.of(StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA)).setFerdigfilterListe(new ArrayList<>())));
        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertEquals(aktorIdCvDeltMedNav.toString(), responseBrukere.getBrukere().getFirst().getAktoerid());
        assertThat(responseBrukere.getBrukere().getFirst().getNesteSvarfristCvStillingFraNav()).isEqualTo(LocalDate.parse("2044-02-03"));
    }

    @Test
    public void hentMoteplan() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();
        VeilederId annenVeileder = randomVeilederId();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, navKontor, veileder, ZonedDateTime.now(), null);
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
        List<Moteplan> moteplaner = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(false, false, false));
        List<Moteplan> ingenMotePlaner = aktivitetService.hentMoteplan(annenVeileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(false, false, false));

        assertThat(moteplaner.size()).isEqualTo(2);
        assertThat(ingenMotePlaner.size()).isEqualTo(0);
    }

    @Test
    public void hentMoteplan_sperretAnsatt() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();

        testDataClient.lagreBrukerUnderOppfolging(aktoer, navKontor, veileder, ZonedDateTime.now(), null);
        settSperretAnsatt(aktoer, navKontor);

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
        List<Moteplan> medTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(false, false, true));
        List<Moteplan> utenTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(false, false, false));

        assertThat(medTilgang.stream().noneMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
        assertThat(utenTilgang.stream().allMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
    }


    @Test
    public void hentMoteplan_diskresjonsKode() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();

        AktorId aktoerKode6 = randomAktorId();
        AktorId aktoerKode7 = randomAktorId();

        testDataClient.lagreBrukerUnderOppfolging(aktoerKode6, navKontor, veileder, ZonedDateTime.now(), "6");
        testDataClient.lagreBrukerUnderOppfolging(aktoerKode7, navKontor, veileder, ZonedDateTime.now(), "7");

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
        List<Moteplan> medTilgang_alt = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(true, true, false));
        List<Moteplan> medTilgang_6 = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(true, false, false));
        List<Moteplan> medTilgang_7 = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(false, true, false));
        List<Moteplan> utenTilgang = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()), new BrukerinnsynTilganger(false, false, false));

        assertThat(medTilgang_alt.stream().noneMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
        assertThat(medTilgang_6.stream().filter(moteplan -> moteplan.deltaker().equals(skjermetDeltaker)).toList().size()).isEqualTo(1);
        assertThat(medTilgang_7.stream().filter(moteplan -> moteplan.deltaker().equals(skjermetDeltaker)).toList().size()).isEqualTo(1);
        assertThat(utenTilgang.stream().allMatch(moteplan -> moteplan.deltaker().equals(skjermetDeltaker))).isTrue();
    }

    private void settSperretAnsatt(AktorId aktorId, NavKontor navKontor) {
        Fnr fnr = pdlIdentRepository.hentFnrForAktivBruker(aktorId);
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null,
                        navKontor.getValue(), null, null,
                        null, ZonedDateTime.now()));
        skjermingRepository.settSkjerming(Fnr.of(fnr.get()), true);
    }

}
