package no.nav.pto.veilarbportefolje.oppfolging;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toTimestamp;
import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OppfolgingServiceTest {

    private JdbcTemplate db;
    private OppfolgingRepository oppfolgingRepository;
    private OppfolgingService oppfolgingService;
    private BrukerRepository brukerRepository;
    private SystemUserTokenProvider systemUserTokenProvider;

    private String FNR = "12345";
    private String AKTORID = "1234567";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Before
    public void setup() {
        systemUserTokenProvider = mock(SystemUserTokenProvider.class);
        brukerRepository = mock(BrukerRepository.class);
        DataSource ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        oppfolgingRepository = new OppfolgingRepository(db, mock(UnleashService.class));
        String apiUrl = "http://localhost:" + wireMockRule.port();
        oppfolgingService = new OppfolgingService(brukerRepository, oppfolgingRepository, systemUserTokenProvider, apiUrl);

    }

    @Test
    public void hentOppfolgingData__skal_lage_riktig_request_og_parse_response() {
        ZonedDateTime startDato_portefolje = ZonedDateTime.now();
        String startDato_portefolje_string = toIsoUTC(startDato_portefolje);

        ZonedDateTime startDato_oppfolging = ZonedDateTime.parse("2021-04-27T10:40:02.110297+02:00");

        when(systemUserTokenProvider.getSystemUserToken()).thenReturn("SYSTEM_TOKEN");
        when(brukerRepository.hentAlleBrukereUnderOppfolging()).thenReturn(
                List.of(new OppfolgingsBruker().setFnr(FNR).setAktoer_id(AKTORID).setOppfolging_startdato(startDato_portefolje_string))
        );

        String response = readFileAsJsonString("/veilarboppfolging_hentOppfolgingsperioder.json");

        givenThat(get(urlEqualTo("/api/oppfolging/oppfolgingsperioder?fnr=" + FNR))
                .withQueryParam("fnr", equalTo(FNR))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer SYSTEM_TOKEN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(response))
        );

        oppfolgingRepository.settUnderOppfolging(AktorId.of(AKTORID), startDato_portefolje);

        List<OppfolgingsBruker> oppfolgingsBruker = brukerRepository.hentAlleBrukereUnderOppfolging();
        oppfolgingsBruker.forEach(oppfolgingService::oppdaterBruker);

        Optional<BrukerOppdatertInformasjon> oppfolgingsData = oppfolgingRepository.hentOppfolgingData(AktorId.of(AKTORID));
        assertThat(oppfolgingsData.get().getStartDato()).isEqualTo(toTimestamp(startDato_oppfolging));

    }

    @SneakyThrows
    private String readFileAsJsonString(String pathname) {
        val URI = getClass().getResource(pathname).toURI();
        val encodedBytes = Files.readAllBytes(Paths.get(URI));
        return new String(encodedBytes, UTF_8).trim();
    }
}
