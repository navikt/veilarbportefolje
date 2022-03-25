package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import no.nav.pto.veilarbportefolje.domene.value.NavKontor;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.I_AKTIVITET;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AktiviteterOpensearchIntegrasjon extends EndToEndTest {
    private final AktivitetService aktivitetService;
    private final OpensearchService opensearchService;
    private final AktorId aktoer = randomAktorId();
    private final Fnr fodselsnummer = Fnr.ofValidFnr("10108000399"); //TESTFAMILIE

    @Autowired
    public AktiviteterOpensearchIntegrasjon(AktivitetService aktivitetService, OpensearchService opensearchService) {
        this.aktivitetService = aktivitetService;
        this.opensearchService = opensearchService;
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
        List<Moteplan> moteplaner = aktivitetService.hentMoteplan(veileder, EnhetId.of(navKontor.getValue()));
        List<Moteplan> ingenMotePlaner = aktivitetService.hentMoteplan(annenVeileder, EnhetId.of(navKontor.getValue()));

        assertThat(moteplaner.size()).isEqualTo(2);
        assertThat(ingenMotePlaner.size()).isEqualTo(0);
    }
}
