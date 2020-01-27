package no.nav.pto.veilarbportefolje.arenafiler;

import io.vavr.control.Try;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class KopierGR199FraArenaTest {

    @Test
    public void unmarshalling() {
        InputStream file = KopierGR199FraArenaTest.class.getResourceAsStream("/arena-vedtak.xml");

        Try<LoependeYtelser> tryYtelse = FilmottakFileUtils.unmarshallFile(file, LoependeYtelser.class);

        assertThat(tryYtelse.isSuccess(), is(true));
    }
}
