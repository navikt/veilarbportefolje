package no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler;

import io.vavr.control.Try;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
public class KopierGR199FraArenaTest {

    @Ignore
    @Test
    public void unmarshalling() {
        InputStream file = KopierGR199FraArenaTest.class.getResourceAsStream("/arena-vedtak.xml");

        Try<LoependeYtelser> tryYtelse = FilmottakFileUtils.unmarshallFile(file, LoependeYtelser.class);

        assertThat(tryYtelse.isSuccess()).isTrue();
    }
}
