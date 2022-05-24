package no.nav.pto.veilarbportefolje.oppfolging;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingServiceTest {
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private OppfolgingService oppfolgingService;
    private SystemUserTokenProvider systemUserTokenProvider;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setup() {
        systemUserTokenProvider = mock(SystemUserTokenProvider.class);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(SingletonPostgresContainer.init().createJdbcTemplate());
        String apiUrl = "http://localhost:" + wireMockRule.port();
        oppfolgingService = new OppfolgingService(mock(OppfolgingAvsluttetService.class), systemUserTokenProvider, apiUrl, oppfolgingRepositoryV2, mock(AktorClient.class));

    }

    @Test
    public void hentOppfolgingData__skal_lage_riktig_request_og_parse_response() {
        String AKTORID = "1234567";
        when(systemUserTokenProvider.getSystemUserToken()).thenReturn("SYSTEM_TOKEN");
        ZonedDateTime startDato_portefolje = ZonedDateTime.now();

        givenThat(get(urlEqualTo("/api/admin/hentVeilarbinfo/bruker?aktorId=" + AKTORID))
                .withQueryParam("aktorId", equalTo(AKTORID))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(readFileAsJsonString("/veilarboppfolging_hentVeilarbinfo.json", getClass())))
        );

        oppfolgingRepositoryV2.settUnderOppfolging(AktorId.of(AKTORID), startDato_portefolje);
        oppfolgingService.oppdaterBruker(AktorId.of(AKTORID));

        Optional<BrukerOppdatertInformasjon> oppfolgingsDataV2_postSync = oppfolgingRepositoryV2.hentOppfolgingData(AktorId.of(AKTORID));
        assertThat(oppfolgingsDataV2_postSync.get().getStartDato()).isEqualTo(toTimestamp(ZonedDateTime.parse("2021-04-27T10:40:02.110297+02:00")));
        assertThat(oppfolgingsDataV2_postSync.get().getManuell()).isTrue();
        assertThat(oppfolgingsDataV2_postSync.get().getNyForVeileder()).isTrue();
        assertThat(oppfolgingsDataV2_postSync.get().getVeileder()).isEqualTo("123");
    }
}
