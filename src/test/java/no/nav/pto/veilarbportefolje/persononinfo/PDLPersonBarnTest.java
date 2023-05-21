package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentResponse;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.Period;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;

public class PDLPersonBarnTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String pdlIdentResponsFraFil = readFileAsJsonString("/identer_pdl.json", getClass());
    private final String pdlIdentBarnResponsFraFil = readFileAsJsonString("/identer_barn_pdl.json", getClass());
    private final String pdlPersonResponsFraFil = readFileAsJsonString("/person_pdl.json", getClass());
    private final String pdlPersonBarnResponsFraFil = readFileAsJsonString("/person_barn_pdl.json", getClass());

    private BarnUnder18AarRepository barnUnder18AarRepository;
    private final JdbcTemplate db;
    private final PdlPersonRepository pdlPersonRepository;

    private PdlService pdlService;

    public PDLPersonBarnTest() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
        pdlPersonRepository = new PdlPersonRepository(db, db);
        barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
    }

    @BeforeEach
    public void setup() {
        WireMockServer server = new WireMockServer();
        db.update("truncate bruker_identer");

        server.stubFor(
                post(anyUrl())
                        .inScenario("PDL test")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(pdlIdentResponsFraFil))
                        .willSetStateTo("hent barn")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarnResponsFraFil))
                .whenScenarioStateIs("hent barn")
        );

        server.start();

        this.pdlService = new PdlService(
                new PdlIdentRepository(db),
                new PdlPersonRepository(db, db),
                new BarnUnder18AarService(new BarnUnder18AarRepository(db, db)),
                new PdlPortefoljeClient(new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM_TOKEN"))
        );
    }

    @Test
    @SneakyThrows
    public void hentOgLagreBrukerDataPaBarnTest() {
        var identerFraFil = mapper.readValue(pdlIdentResponsFraFil, PdlIdentResponse.class)
                .getData()
                .getHentIdenter()
                .getIdenter();

       pdlService.hentOgLagreIdenter(randomAktorId());

        Fnr fnr = hentAktivFnr(identerFraFil);
        pdlService.hentOgLagreBrukerDataPaBarn(fnr);
        BarnUnder18AarData barn = barnUnder18AarRepository.hentInfoOmBarn(fnr);
        LocalDate fDato = LocalDate.of(2014, 07, 07);
        Integer alder = Period.between(fDato, LocalDate.now()).getYears();

        assertThat(barn.getAlder()).isEqualTo(alder.longValue());
        assertThat(barn.getDiskresjonskode()).isEqualTo(null);
    }
}
