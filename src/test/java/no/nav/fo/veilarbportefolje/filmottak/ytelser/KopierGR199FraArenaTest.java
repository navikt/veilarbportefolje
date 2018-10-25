package no.nav.fo.veilarbportefolje.filmottak.ytelser;

import io.vavr.control.Try;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;

import static java.lang.System.setProperty;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.LOEPENDEYTELSER_FILNAVN_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.LOEPENDEYTELSER_PATH_PROPERTY;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class KopierGR199FraArenaTest {
    private static InputStream vedtakFormat = KopierGR199FraArenaTest.class.getResourceAsStream("/arena-vedtak.xml");

    @BeforeClass
    public static void setup() {
        setProperty(LOEPENDEYTELSER_PATH_PROPERTY, "/");
        setProperty(LOEPENDEYTELSER_FILNAVN_PROPERTY, "test.xml");
    }

    @Test
    public void unmarshalling() {
        Try<LoependeYtelser> tryYtelse = KopierGR199FraArena.unmarshall(vedtakFormat);

        assertThat(tryYtelse.isSuccess(), is(true));
    }
}
