package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arenapakafka.ArenaDato;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakService;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.auth.Skjermettilgang;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerEntity;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.I_AKTIVITET;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.I_AVTALT_AKTIVITET;
import static no.nav.pto.veilarbportefolje.domene.Motedeltaker.skjermetDeltaker;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AktiviteterOpensearchIntegrasjon extends EndToEndTest {
    private final AktivitetService aktivitetService;
    private final TiltakService tiltakService;
    private final OpensearchService opensearchService;
    private final OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository;
    private final AktorId aktoer = randomAktorId();
    private final Fnr fodselsnummer = Fnr.ofValidFnr("10108000399"); //TESTFAMILIE
    private final JdbcTemplate jdbcTemplatePostgres;
    private final PdlIdentRepository pdlIdentRepository;

    private final TiltakRepositoryV2 tiltakRepositoryV2;

    @Autowired
    public AktiviteterOpensearchIntegrasjon(AktivitetService aktivitetService, TiltakService tiltakService, OpensearchService opensearchService, OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepository, JdbcTemplate jdbcTemplatePostgres, PdlIdentRepository pdlIdentRepository, TiltakRepositoryV2 tiltakRepositoryV2) {
        this.aktivitetService = aktivitetService;
        this.tiltakService = tiltakService;
        this.opensearchService = opensearchService;
        this.oppfolgingsbrukerRepository = oppfolgingsbrukerRepository;
        this.jdbcTemplatePostgres = jdbcTemplatePostgres;
        this.pdlIdentRepository = pdlIdentRepository;
        this.tiltakRepositoryV2 = tiltakRepositoryV2;
    }

    @BeforeEach
    public void resetDb() {
        jdbcTemplatePostgres.update("TRUNCATE aktiviteter");
        jdbcTemplatePostgres.update("TRUNCATE oppfolgingsbruker_arena_v2");
        jdbcTemplatePostgres.update("TRUNCATE bruker_identer");
        jdbcTemplatePostgres.update("TRUNCATE oppfolging_data");
    }

    @Test
    public void lasteroppeikkelagreteaktiviteteter() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
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
    public void lasterOppTiltaksaktivitet() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(1L)
                .setAvtalt(true));
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");
                }
        );
    }

    @Test
    public void lasterOppTiltaksaktivitet2() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(1L)
                .setAvtalt(true));

        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukere.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");
                }
        );
    }

    @Test
    public void versjonsnummerTiltaksaktivitet() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("LONNTILS")
                .setVersion(1L)
                .setAvtalt(true));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(2L)
                .setAvtalt(true));


        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukereMIDLONTIL = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

            BrukereMedAntall responseBrukereLONNTILS = opensearchService.hentBrukere(
                    navKontor.getValue(),
                    empty(),
                    "asc",
                    "ikke_satt",
                    new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("LONNTILS")),
                    null,
                    null);

                    assertThat(responseBrukereMIDLONTIL.getAntall()).isEqualTo(1);
                    assertThat(responseBrukereLONNTILS.getAntall()).isEqualTo(0);
                    assertThat(responseBrukereMIDLONTIL.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");

                }
        );
    }


    @Test
    public void skal_laste_inn_samme_data_fra_ny_datakilde_korrekt() {
        String tiltaksType1 = "LONNTILS";
        String tiltaksNavn1 = "Lønnstilskudd";
        String tiltaksType2 = "MIDLONTIL";
        String tiltaksNavn2 = "Midlertidig lønnstilskudd";

        TiltakInnhold tiltak1 = new TiltakInnhold()
                .setFnr(fodselsnummer.toString())
                .setTiltaksnavn(tiltaksNavn1)
                .setTiltakstype(tiltaksType1)
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2018-10-03"))
                .setAktivitetperiodeTil(new ArenaDato("2024-11-01"))
                .setAktivitetid("2");
        tiltakRepositoryV2.upsert(tiltak1, aktoer);

        TiltakInnhold tiltak2 = new TiltakInnhold()
                .setFnr(fodselsnummer.toString())
                .setTiltaksnavn(tiltaksNavn2)
                .setTiltakstype(tiltaksType2)
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeFra(new ArenaDato("2017-02-03"))
                .setAktivitetperiodeTil(new ArenaDato("2023-02-03"))
                .setAktivitetid("3");
        tiltakRepositoryV2.upsert(tiltak2, aktoer);

        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2018-10-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2024-11-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2021-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(tiltaksType1)
                .setVersion(1L)
                .setAvtalt(true));
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("3")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.of(LocalDate.parse("2017-02-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setTilDato(ZonedDateTime.of(LocalDate.parse("2023-02-03"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setEndretDato(ZonedDateTime.of(LocalDate.parse("2021-01-01"), LocalTime.parse("00:00:00"), ZoneId.systemDefault()))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode(tiltaksType2)
                .setVersion(1L)
                .setAvtalt(true));


        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukereMIDLONTIL = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukereMIDLONTIL.getAntall()).isEqualTo(1);
                    assertThat(responseBrukereMIDLONTIL.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");

                }
        );
    }

    @Test
    public void brukertiltakErOppdatert() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer.get())
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.TILTAK)
                .setFraDato(ZonedDateTime.now())
                .setTilDato(ZonedDateTime.parse("2023-02-03T10:10:10+02:00"))
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setTiltakskode("MIDLONTIL")
                .setVersion(1L)
                .setAvtalt(true));
        verifiserAsynkront(5, TimeUnit.SECONDS, () -> {
                    BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                            navKontor.getValue(),
                            empty(),
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setFerdigfilterListe(List.of()).setTiltakstyper(List.of("MIDLONTIL")),
                            null,
                            null);

                    assertThat(responseBrukere.getBrukere().get(0).getBrukertiltak().get(0)).isEqualTo("MIDLONTIL");
                }
        );
    }

    @Test
    public void lasteroppaktivitetStillingFraNAV() {
        NavKontor navKontor = randomNavKontor();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, fodselsnummer, navKontor.getValue());
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
                            "asc",
                            "ikke_satt",
                            new Filtervalg().setNavnEllerFnrQuery(fodselsnummer.toString()).setFerdigfilterListe(new ArrayList<>()),
                            null,
                            null);

                    assertThat(responseBrukere.getAntall()).isEqualTo(1);
                    assertThat(responseBrukere.getBrukere().get(0).getNesteCvKanDelesStatus()).isEqualTo("JA");
                    assertThat(responseBrukere.getBrukere().get(0).getNesteSvarfristCvStillingFraNav()).isEqualTo(LocalDate.parse("2044-02-03"));
                }
        );
    }

    @Test
    public void filtrerBrukerePaStillingFraNAV() {
        NavKontor navKontor = randomNavKontor();
        AktorId aktoer1 = randomAktorId();
        AktorId aktoer2 = randomAktorId();
        VeilederId veileder = randomVeilederId();
        testDataClient.lagreBrukerUnderOppfolging(aktoer1, navKontor, veileder, ZonedDateTime.now());
        testDataClient.lagreBrukerUnderOppfolging(aktoer2, navKontor, veileder, ZonedDateTime.now());
        aktivitetService.behandleKafkaMeldingLogikk(new KafkaAktivitetMelding()
                .setAktivitetId("2")
                .setAktorId(aktoer1.toString())
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
                .setAktorId(aktoer2.toString())
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
                    "asc",
                    "ikke_satt",
                    new Filtervalg().setFerdigfilterListe(List.of(I_AKTIVITET)),
                    null,
                    null);

            assertThat(responseBrukere.getAntall()).isEqualTo(2);
        });



        BrukereMedAntall responseBrukere = opensearchService.hentBrukere(
                navKontor.getValue(),
                empty(),
                "asc",
                "ikke_satt",
                new Filtervalg().setStillingFraNavFilter(List.of(StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA)).setFerdigfilterListe(new ArrayList<>()),
                null,
                null);

        System.out.println(JsonUtils.toJson( new Filtervalg().setStillingFraNavFilter(List.of(StillingFraNAVFilter.CV_KAN_DELES_STATUS_JA)).setFerdigfilterListe(new ArrayList<>())));
        assertThat(responseBrukere.getAntall()).isEqualTo(1);
        assertThat(responseBrukere.getBrukere().get(0).getNesteCvKanDelesStatus()).isEqualTo("JA");
        assertThat(responseBrukere.getBrukere().get(0).getNesteSvarfristCvStillingFraNav()).isEqualTo(LocalDate.parse("2044-02-03"));


    }

    @Test
    public void hentMoteplan() {
        NavKontor navKontor = randomNavKontor();
        VeilederId veileder = randomVeilederId();
        VeilederId annenVeileder = randomVeilederId();
        testDataClient.lagreBrukerUnderOppfolging(aktoer, navKontor, veileder, ZonedDateTime.now());
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

        testDataClient.lagreBrukerUnderOppfolging(aktoer, navKontor, veileder, ZonedDateTime.now());
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

        testDataClient.lagreBrukerUnderOppfolging(aktoerKode6, navKontor, veileder, ZonedDateTime.now());
        testDataClient.lagreBrukerUnderOppfolging(aktoerKode7, navKontor, veileder, ZonedDateTime.now());
        settDiskresjonskode(aktoerKode6, navKontor, "6");
        settDiskresjonskode(aktoerKode7, navKontor, "7");

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

    private void settSperretAnsatt(AktorId aktorId, NavKontor navKontor) {
        Fnr fnr = pdlIdentRepository.hentFnr(aktorId);
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null,
                        "test", "testson", navKontor.getValue(), null, null,
                        null, null, null, true,
                        false, ZonedDateTime.now()));
    }

    private void settDiskresjonskode(AktorId aktorId, NavKontor navKontor, String kode) {
        Fnr fnr = pdlIdentRepository.hentFnr(aktorId);
        oppfolgingsbrukerRepository.leggTilEllerEndreOppfolgingsbruker(
                new OppfolgingsbrukerEntity(fnr.get(), null, null,
                        "test", "testson", navKontor.getValue(), null, null,
                        null, null, kode, false, false,
                        ZonedDateTime.now()));
    }
}
