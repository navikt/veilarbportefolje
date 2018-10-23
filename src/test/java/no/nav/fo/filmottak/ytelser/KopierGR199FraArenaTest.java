package no.nav.fo.filmottak.ytelser;

import io.vavr.control.Try;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class KopierGR199FraArenaTest {
    private static InputStream vedtakFormat = KopierGR199FraArenaTest.class.getResourceAsStream("arena-vedtak.xml");

    @Test
    public void unmarshalling() {
        Try<LoependeYtelser> tryYtelse = KopierGR199FraArena.unmarshall(vedtakFormat);

        assertThat(tryYtelse.isSuccess(), is(true));
    }
}