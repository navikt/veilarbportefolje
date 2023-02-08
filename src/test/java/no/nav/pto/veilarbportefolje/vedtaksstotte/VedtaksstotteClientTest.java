package no.nav.pto.veilarbportefolje.vedtaksstotte;


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakApiDto;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal.BEHOLDE_ARBEID;
import static no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS;
import static org.assertj.core.api.Assertions.assertThat;

public class VedtaksstotteClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void hentSiste14aVedtak__gir_forventet_respons() {
        Fnr fnr = Fnr.of("123");

        String url = "http://localhost:" + wireMockRule.port();
        VedtaksstotteClient client = new VedtaksstotteClient(url, null, () -> "TOKEN", null);

        String responseBody = """
                    {
                        "innsatsgruppe": "SITUASJONSBESTEMT_INNSATS",
                        "hovedmal": "BEHOLDE_ARBEID",
                        "fattetDato": "2022-06-12T10:32:29.36+02:00",
                        "fraArena": true
                    }
                """;

        givenThat(get(urlEqualTo("/api/siste-14a-vedtak?fnr=" + fnr)).willReturn(aResponse().withStatus(200).withBody(responseBody)));

        Optional<Siste14aVedtakApiDto> response = client.hentSiste14aVedtak(fnr);

        Siste14aVedtakApiDto forventet = new Siste14aVedtakApiDto(
                SITUASJONSBESTEMT_INNSATS,
                BEHOLDE_ARBEID,
                ZonedDateTime.parse("2022-06-12T10:32:29.36+02:00"),
                true
        );

        assertThat(response).isEqualTo(Optional.of(forventet));
    }
}
