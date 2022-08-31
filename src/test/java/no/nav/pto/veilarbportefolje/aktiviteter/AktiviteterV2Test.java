package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.postgres.AktivitetOpensearchService;
import no.nav.pto.veilarbportefolje.postgres.utils.AvtaltAktivitetEntity;
import no.nav.pto.veilarbportefolje.postgres.PostgresAktivitetMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.List;

import static java.lang.String.valueOf;
import static java.util.concurrent.ThreadLocalRandom.current;
import static no.nav.pto.veilarbportefolje.util.DateUtils.FAR_IN_THE_FUTURE_DATE;
import static no.nav.pto.veilarbportefolje.util.DateUtils.now;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class AktiviteterV2Test {
    private final AktivitetOpensearchService aktivitetOpensearchService;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final AktivitetService aktivitetService;

    @Autowired
    public AktiviteterV2Test(AktivitetOpensearchService aktivitetOpensearchService, AktiviteterRepositoryV2 aktiviteterRepositoryV2, AktivitetService aktivitetService) {
        this.aktivitetOpensearchService = aktivitetOpensearchService;
        this.aktiviteterRepositoryV2 = aktiviteterRepositoryV2;
        this.aktivitetService = aktivitetService;
    }

    @Test
    public void skal_komme_i_aktivitet_V2() {
        final AktorId aktorId = randomAktorId();
        final ZonedDateTime fraDato = now().plusDays(1);
        final ZonedDateTime tilDato = now().plusDays(2);
        KafkaAktivitetMelding aktivitet = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setFraDato(fraDato)
                .setTilDato(tilDato);
        aktivitetService.behandleKafkaMeldingLogikk(aktivitet);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        //Opensearch mapping
        Assertions.assertThat(postgresAktivitet.getTiltak().size()).isEqualTo(0);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().size()).isEqualTo(1);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.egen.name())).isTrue();

        Assertions.assertThat(postgresAktivitet.getNyesteUtlopteAktivitet()).isNull();
        Assertions.assertThat(postgresAktivitet.getForrigeAktivitetStart()).isNull();

        Assertions.assertThat(postgresAktivitet.getAktivitetTiltakUtlopsdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
        Assertions.assertThat(postgresAktivitet.getNesteAktivitetStart()).isNull();
        Assertions.assertThat(postgresAktivitet.getAktivitetStart()).isEqualTo(toIsoUTC(fraDato));
        Assertions.assertThat(postgresAktivitet.getAktivitetEgenUtlopsdato()).isEqualTo(toIsoUTC(tilDato));
    }

    @Test
    public void mote_idag_er_aktivt() {
        final AktorId aktorId = randomAktorId();
        final ZonedDateTime fraDato = now();
        final ZonedDateTime tilDato = now().plusSeconds(2);

        KafkaAktivitetMelding aktivitet = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setFraDato(fraDato)
                .setTilDato(tilDato);

        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        //Opensearch mapping
        Assertions.assertThat(postgresAktivitet.getAktiviteter().size()).isEqualTo(1);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.mote.name())).isTrue();

        Assertions.assertThat(postgresAktivitet.getNyesteUtlopteAktivitet()).isNull();
        Assertions.assertThat(postgresAktivitet.getAktivitetStart()).isEqualTo(toIsoUTC(fraDato));

        Assertions.assertThat(postgresAktivitet.getAktivitetMoteUtlopsdato()).isEqualTo(toIsoUTC(tilDato));
        Assertions.assertThat(postgresAktivitet.getAktivitetMoteStartdato()).isEqualTo(toIsoUTC(fraDato));
    }

    @Test
    public void mote_igår_er_ikke_aktivt() {
        final AktorId aktorId = randomAktorId();
        final ZonedDateTime fraDato = now().minusDays(1);
        final ZonedDateTime tilDato = now().minusDays(1).plusSeconds(2);

        KafkaAktivitetMelding aktivitet = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setFraDato(fraDato)
                .setTilDato(tilDato);

        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        //Opensearch mapping
        Assertions.assertThat(postgresAktivitet.getAktiviteter().size()).isEqualTo(1);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.mote.name())).isTrue();
        Assertions.assertThat(postgresAktivitet.getNyesteUtlopteAktivitet()).isEqualTo(toIsoUTC(tilDato));

        Assertions.assertThat(postgresAktivitet.getAktivitetMoteUtlopsdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
        Assertions.assertThat(postgresAktivitet.getAktivitetMoteStartdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
    }

    @Test
    public void skal_kunne_ha_flere_typer_aktiviteter_V2() {
        final AktorId aktorId = randomAktorId();
        final ZonedDateTime fraDato1 = now().plusDays(1);
        final ZonedDateTime tilDato1 = now().plusDays(2);
        final ZonedDateTime fraDato2 = now().plusDays(3);
        final ZonedDateTime tilDato2 = now().plusDays(4);

        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.BEHANDLING)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setFraDato(fraDato1)
                .setTilDato(tilDato1);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES)
                .setFraDato(fraDato2)
                .setTilDato(tilDato2)
                .setStillingFraNavData(
                        new KafkaAktivitetMelding.StillingFraNAV()
                                .setCvKanDelesStatus(KafkaAktivitetMelding.CvKanDelesStatus.IKKE_SVART)
                                .setSvarfrist("2065-02-03T00:00:00+02:00")
                );

        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet1);
        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet2);

        AvtaltAktivitetEntity postgresAktivitet = PostgresAktivitetMapper.kalkulerAvtalteAktivitetInformasjon(aktivitetOpensearchService
                .hentAvtaltAktivitetData(List.of(aktorId))
                .get(aktorId));

        //Opensearch mapping
        Assertions.assertThat(postgresAktivitet.getTiltak().size()).isEqualTo(0);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().size()).isEqualTo(2);
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.behandling.name())).isTrue();
        Assertions.assertThat(postgresAktivitet.getAktiviteter().contains(AktivitetsType.mote.name())).isTrue();

        Assertions.assertThat(postgresAktivitet.getNyesteUtlopteAktivitet()).isNull();
        Assertions.assertThat(postgresAktivitet.getForrigeAktivitetStart()).isNull();
        Assertions.assertThat(postgresAktivitet.getAktivitetStart()).isEqualTo(toIsoUTC(fraDato1));
        Assertions.assertThat(postgresAktivitet.getNesteAktivitetStart()).isEqualTo(toIsoUTC(fraDato2));

        Assertions.assertThat(postgresAktivitet.getAktivitetTiltakUtlopsdato()).isEqualTo(FAR_IN_THE_FUTURE_DATE);
        Assertions.assertThat(postgresAktivitet.getAktivitetBehandlingUtlopsdato()).isEqualTo(toIsoUTC(tilDato1));
        Assertions.assertThat(postgresAktivitet.getAktivitetMoteUtlopsdato()).isEqualTo(toIsoUTC(tilDato2));
        Assertions.assertThat(postgresAktivitet.getAktivitetMoteStartdato()).isEqualTo(toIsoUTC(fraDato2));
    }
}
