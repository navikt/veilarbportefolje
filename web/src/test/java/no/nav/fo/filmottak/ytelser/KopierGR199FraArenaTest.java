package no.nav.fo.filmottak.ytelser;

import io.vavr.control.Try;
import no.nav.fo.loependeytelser.LoependeVedtak;
import no.nav.fo.loependeytelser.LoependeYtelser;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class KopierGR199FraArenaTest {
    private static InputStream gammeltFormat = KopierGR199FraArenaTest.class.getResourceAsStream("gammelt-format.xml");
    private static InputStream nyttFormat = KopierGR199FraArenaTest.class.getResourceAsStream("nytt-format.xml");


    @Test
    public void gammeltFormatTest() throws Exception {
        Try<LoependeYtelser> tryYtelse = KopierGR199FraArena.unmarshall(gammeltFormat);
        List<LoependeVedtak> vedtaksListe = getLoependeVedtaksListe(tryYtelse);

        assertThat(vedtaksListe.size(), is(1));

        LoependeVedtak loependeVedtak = vedtaksListe.get(0);

        assertThat(loependeVedtak.getAaptellere().getAntallUkerIgjen().intValue(), is(133));
        assertThat(loependeVedtak.getAaptellere().getAntallDagerIgjen().intValue(), is(3));
        assertThat(loependeVedtak.getAaptellere().getAntallDagerUnntak().intValue(), is(5));
        assertNull(loependeVedtak.getAaptellere().getAntallDagerIgjenUnntak());

        assertTrue(tryYtelse.isSuccess());
    }

    @Test
    public void nyttFormatTest() throws Exception {
        Try<LoependeYtelser> tryYtelse = KopierGR199FraArena.unmarshall(nyttFormat);
        List<LoependeVedtak> vedtaksListe = getLoependeVedtaksListe(tryYtelse);

        assertThat(vedtaksListe.size(), is(1));

        LoependeVedtak loependeVedtak = vedtaksListe.get(0);

        assertThat(loependeVedtak.getAaptellere().getAntallUkerIgjen().intValue(), is(133));
        assertThat(loependeVedtak.getAaptellere().getAntallDagerIgjen().intValue(), is(3));
        assertThat(loependeVedtak.getAaptellere().getAntallDagerIgjenUnntak().intValue(), is(5));
        assertNull(loependeVedtak.getAaptellere().getAntallDagerUnntak());

        assertTrue(tryYtelse.isSuccess());
    }

    private List<LoependeVedtak> getLoependeVedtaksListe(Try<LoependeYtelser> tryYtelse) {
        List<LoependeVedtak> vedtaksListe = new ArrayList<>();

        tryYtelse
                .map(LoependeYtelser::getLoependeVedtakListe)
                .andThen(vedtaksListe::addAll);
        return vedtaksListe;
    }
}