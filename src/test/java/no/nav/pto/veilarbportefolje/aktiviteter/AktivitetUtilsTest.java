package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.domene.Motedeltaker;
import no.nav.pto.veilarbportefolje.domene.MoteplanDTO;
import no.nav.pto.veilarbportefolje.domene.Moteplan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.time.*;

import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.aktiviteter.AktivitetUtils.finnNyesteUtlopteAktivAktivitet;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class AktivitetUtilsTest {

    @Test
    public void skalFinneNyesteUtlopteAktivteAktivitet() {
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetIkkeAktivStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.now();

        Timestamp denNyesteAktiviteten = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        Timestamp denEldsteAktiviteten = Timestamp.valueOf(LocalDateTime.now().minusDays(2));

        Timestamp nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);
        assertThat(nyesteIkkeFullforte).isEqualTo(denNyesteAktiviteten);
    }

    @Test
    public void skalReturnereNullNaarDetIkkeFinnesNoenUtlopteAktiviteter() {
        String ikkeFullfortStatus = "enStatusSomIkkeErfullfort";
        assertThat(AktivitetIkkeAktivStatuser.contains(ikkeFullfortStatus)).isFalse();
        LocalDate today = LocalDate.parse("2017-05-01");

        Timestamp denNyesteAktiviteten = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        Timestamp denEldsteAktiviteten = Timestamp.valueOf(LocalDateTime.now().minusDays(2));

        Timestamp nyesteIkkeFullforte = finnNyesteUtlopteAktivAktivitet(asList(denEldsteAktiviteten, denNyesteAktiviteten), today);

        assertThat(nyesteIkkeFullforte).isNull();
    }

    @Test
    public void skalMappeMellomMoteplanOgMoteplanDTO() {
        Motedeltaker deltaker = new Motedeltaker("Test", "Testesen", "01010112345");
        String dato = "2025-08-21T09:00:00Z";
        ZonedDateTime moteFra = ZonedDateTime.now();
        ZonedDateTime moteTil = moteFra.plusMinutes(123);

        Moteplan moteplan = new Moteplan(deltaker, dato, moteFra, moteTil, false);
        MoteplanDTO forventet = new MoteplanDTO(deltaker, dato, 123, false);

        MoteplanDTO result = MoteplanDTO.of(moteplan);

        assertThat(result).isEqualTo(forventet);
    }
}
