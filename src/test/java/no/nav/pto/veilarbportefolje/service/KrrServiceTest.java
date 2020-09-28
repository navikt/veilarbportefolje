package no.nav.pto.veilarbportefolje.service;

import io.vavr.control.Option;
import org.junit.Test;

import java.sql.Timestamp;

import static no.nav.pto.veilarbportefolje.krr.KrrService.nyesteAv;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
}
