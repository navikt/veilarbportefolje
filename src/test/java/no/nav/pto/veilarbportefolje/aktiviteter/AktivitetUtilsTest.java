package no.nav.pto.veilarbportefolje.aktiviteter;

import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
