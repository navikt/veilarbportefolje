package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.lang.String.valueOf;
import static java.util.concurrent.ThreadLocalRandom.current;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class AktiviteterV2Test {
    private final AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2;
    private final AktiviteterRepositoryV2 aktiviteterRepositoryV2;
    private final AktivitetService aktivitetService;

    @Autowired
    public AktiviteterV2Test(AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2, AktiviteterRepositoryV2 aktiviteterRepositoryV2, AktivitetService aktivitetService) {
        this.aktivitetStatusRepositoryV2 = aktivitetStatusRepositoryV2;
        this.aktiviteterRepositoryV2 = aktiviteterRepositoryV2;
        this.aktivitetService = aktivitetService;
    }

    @Test
    public void skal_komme_i_aktivitet_V2() {
        final AktorId aktorId = randomAktorId();
        KafkaAktivitetMelding aktivitet = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);
        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet);
        aktivitetService.utleddAktivitetStatuser(aktorId, KafkaAktivitetMelding.AktivitetTypeData.EGEN);
        AktoerAktiviteter avtalteAktiviteterForAktoerid = aktiviteterRepositoryV2.getAktiviteterForAktoerid(aktorId, false);
        Optional<AktivitetStatus> aktivitettypeStatus = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), "egen");

        assertThat(avtalteAktiviteterForAktoerid.getAktiviteter().size()).isEqualTo(1);
        assertThat(aktivitettypeStatus).isPresent();
        assertThat(aktivitettypeStatus.get().aktiv).isTrue();
    }

    @Test
    public void skal_kunne_ha_flere_typer_aktiviteter_V2() {
        final AktorId aktorId = randomAktorId();
        KafkaAktivitetMelding aktivitet1 = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.EGEN)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);

        KafkaAktivitetMelding aktivitet2 = new KafkaAktivitetMelding()
                .setAktivitetId(valueOf(current().nextInt()))
                .setVersion(1L)
                .setAktivitetType(KafkaAktivitetMelding.AktivitetTypeData.MOTE)
                .setAktorId(aktorId.get())
                .setAvtalt(true)
                .setEndretDato(ZonedDateTime.parse("2017-02-03T10:10:10+02:00"))
                .setAktivitetStatus(KafkaAktivitetMelding.AktivitetStatus.GJENNOMFORES);

        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet1);
        aktiviteterRepositoryV2.tryLagreAktivitetData(aktivitet2);
        aktivitetService.utleddAktivitetStatuser(aktorId, KafkaAktivitetMelding.AktivitetTypeData.EGEN);
        aktivitetService.utleddAktivitetStatuser(aktorId, KafkaAktivitetMelding.AktivitetTypeData.MOTE);

        AktoerAktiviteter avtalteAktiviteterForAktoerid = aktiviteterRepositoryV2.getAktiviteterForAktoerid(aktorId, true);
        Optional<AktivitetStatus> aktivitettypeStatus1 = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), "egen");
        Optional<AktivitetStatus> aktivitettypeStatus2 = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), "mote");

        assertThat(avtalteAktiviteterForAktoerid.getAktiviteter().size()).isEqualTo(2);
        assertThat(aktivitettypeStatus1).isPresent();
        assertThat(aktivitettypeStatus1.get().aktiv).isTrue();
        assertThat(aktivitettypeStatus2).isPresent();
        assertThat(aktivitettypeStatus2.get().aktiv).isTrue();
    }
}
