package no.nav.pto.veilarbportefolje.persononinfo;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;

public class PdlPortefoljeClientTest {

    private final String pdlPersonBarnBolkResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn_bolk_2.json", getClass());

    private PdlPortefoljeClient pdlPortefoljeClient;
    private final WireMockServer server = new WireMockServer();

    @BeforeEach
    public void setup() {
                server.stubFor(
                post(anyUrl())
                        .inScenario("PDL test")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(pdlPersonBarnBolkResponsFraFil))
        );

        server.start();

        this.pdlPortefoljeClient = new PdlPortefoljeClient(
                new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM_TOKEN"));
    }

    @AfterEach
    public void stopServer(){
        server.stop();
    }

    @Test
    public void testHentingBolkDataForBarn(){
        Map<Fnr, PDLPersonBarn> pdlPersonBarn = pdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(List.of(randomFnr(), randomFnr(), randomFnr()));

        Assertions.assertEquals(3, pdlPersonBarn.size());

        Assertions.assertTrue(pdlPersonBarn.containsKey(Fnr.of("18501550031")));
        Assertions.assertTrue(pdlPersonBarn.containsKey(Fnr.of("09421880325")));
        Assertions.assertTrue(pdlPersonBarn.containsKey(Fnr.of("02481081249")));

        Assertions.assertTrue(pdlPersonBarn.get(Fnr.of("18501550031")).isErIlive());
        Assertions.assertEquals("6", pdlPersonBarn.get(Fnr.of("18501550031")).getDiskresjonskode());
        Assertions.assertEquals("2015-10-18", pdlPersonBarn.get(Fnr.of("18501550031")).getFodselsdato().toString());

        Assertions.assertTrue(pdlPersonBarn.get(Fnr.of("09421880325")).isErIlive());
        Assertions.assertNull(pdlPersonBarn.get(Fnr.of("09421880325")).getDiskresjonskode());
        Assertions.assertEquals("2018-02-09", pdlPersonBarn.get(Fnr.of("09421880325")).getFodselsdato().toString());

        Assertions.assertTrue(pdlPersonBarn.get(Fnr.of("02481081249")).isErIlive());
        Assertions.assertNull(pdlPersonBarn.get(Fnr.of("02481081249")).getDiskresjonskode());
        Assertions.assertEquals("2010-08-02", pdlPersonBarn.get(Fnr.of("02481081249")).getFodselsdato().toString());
    }

}