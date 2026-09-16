package no.nav.pto.veilarbportefolje.kodeverk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.pto.veilarbportefolje.kodeverk.KodeverkService.KODEVERK_SPRAAK;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readTestResourceFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest
public class KodeverkClientImplTest {

    @Test
    public void testFindingMostRecentValue(WireMockRuntimeInfo wireMockRuntimeInfo) {
        String kodeverkJson = readTestResourceFile("kodeverk-spraak.json");
        String apiUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        KodeverkClientImpl kodeverkClient = new KodeverkClientImpl(apiUrl, () -> "TOKEN");

        givenThat(get(anyUrl())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(kodeverkJson))
        );
        Map<String, String> kodeverkBeskrivelser = kodeverkClient.hentKodeverkBeskrivelser(KODEVERK_SPRAAK);
        assertEquals("Hindi", kodeverkBeskrivelser.get("HI"));
        assertEquals("Pushto", kodeverkBeskrivelser.get("PS"));
        assertEquals("Portugesisk", kodeverkBeskrivelser.get("PT"));
        assertEquals("Test", kodeverkBeskrivelser.get("TST"));
    }

}