package no.nav.fo.service;

import io.vavr.control.Option;
import no.nav.fo.database.KrrRepository;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static no.nav.fo.service.KrrService.nyesteAv;
import static no.nav.fo.util.DateUtils.timestampFromISO8601;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KrrServiceTest {

    Timestamp epostTimestamp = new Timestamp(timestampFromISO8601("2018-05-10T10:10:10+02:00").getTime());
    Timestamp mobilTimestamp = new Timestamp(timestampFromISO8601("2018-05-11T10:10:10+02:00").getTime());

    Option<Timestamp> epostSisteVerifisert = Option.of(epostTimestamp);
    Option<Timestamp> mobilSisteVerifisert = Option.of(mobilTimestamp);

    @Test
    void nyesteAvEpostOgMobilsistVerifisert() {
        Timestamp nyesteAvHvisEpostOgMobilEksisterer = nyesteAv(epostSisteVerifisert, mobilSisteVerifisert);
        assertThat(nyesteAvHvisEpostOgMobilEksisterer).isEqualTo(mobilTimestamp);
    }

    @Test
    void nyesteAvHvisEpostEksistererIkke() {
        Timestamp nyesteAvHvisEpostErNull = nyesteAv(Option.none(), mobilSisteVerifisert);
        assertThat(nyesteAvHvisEpostErNull).isEqualTo(mobilTimestamp);
    }

    @Test
    void nyesteAvHvisMobilEksistererIkke() {
        Timestamp nyesteAvHvisMobilErNull = nyesteAv(epostSisteVerifisert, Option.none());
        assertThat(nyesteAvHvisMobilErNull).isEqualTo(epostTimestamp);
    }

    @Test
    void nyesteAvHvisEpostOgMobilErLike() {
        Timestamp nyesteAvHvisEpostOgMobilErLike = nyesteAv(epostSisteVerifisert, epostSisteVerifisert);
        assertThat(nyesteAvHvisEpostOgMobilErLike).isEqualTo(epostTimestamp);
    }

    @Test
    void nyesteAvHvisEpostOgMobilErNull() {
        Timestamp nyesteAvHvisEpostOgMobilErNull = nyesteAv(Option.none(), Option.none());
        assertThat(nyesteAvHvisEpostOgMobilErNull).isEqualTo(null);
    }

    @Test
    void skalIkkeKasteExceptionsVedFeilMotKRR() throws Exception {
        KrrRepository repo = mock(KrrRepository.class);
        DigitalKontaktinformasjonV1 dkif = mock(DigitalKontaktinformasjonV1.class);
        KrrService service = new KrrService(repo, dkif);

        when(dkif.hentDigitalKontaktinformasjonBolk(any())).thenThrow(SocketTimeoutException.class);

        try {
            service.hentDigitalKontaktInformasjon(new ArrayList<>());
        } catch (Exception e) {
            fail(e.getMessage(), e);
        }

        verify(repo, never()).lagreKRRInformasjon(any());
    }
}
