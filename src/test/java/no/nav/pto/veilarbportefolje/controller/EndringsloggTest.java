package no.nav.pto.veilarbportefolje.controller;

import no.nav.pto.veilarbportefolje.mock.MetricsClientMock;
import org.junit.Test;

public class EndringsloggTest {
    Frontendlogger frontendlogger = new Frontendlogger(new MetricsClientMock());

    @Test
    public void testLogger() {
        frontendlogger.skrivEventTilInflux(new Frontendlogger.FrontendEvent().setName("test"));
    }
}
