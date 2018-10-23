package no.nav.fo.veilarbportefolje.pacts;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.veilarbportefolje.domene.VeilederId;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static java.lang.String.format;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ExtendWith(PactConsumerTestExt.class)
public class PactVeilederInfoConsumerTest {

    private static final String VEILARB_PORTEFOLJE = "veilarbportefolje";
    private static final String VEILARBVEILEDER_VEILEDER_API = "/veilarbveileder/api/veileder";
    private static final String VEILARBVEILEDER_ENHET_API = "/veilarbveileder/api/enhet";
    private static final String VEILARB_VEILEDER = "veilarbveileder";
    private static final String VEILARB_ENHET = "0106";
    private static final String IDENTER = "identer";
    private static final String FASIT_ALIAS = "priveligert_veileder";

    private static final String VEILEDER_ID = Optional.ofNullable(System.getenv("PACT_VEILEDER_ID"))
            .orElse(FasitUtils.getTestUser(FASIT_ALIAS, FasitUtils.getDefaultEnvironment()).getUsername());

    @Pact(provider = VEILARB_VEILEDER, consumer = VEILARB_PORTEFOLJE)
    public RequestResponsePact createPactVeilederInfoFinnes(PactDslWithProvider builder) {
        return builder
                .given("a request for info about a veileder")
                .uponReceiving("request about an existing veileder")
                    .matchPath(VEILARBVEILEDER_VEILEDER_API + "/\\w{7,}", format("%s/%s", VEILARBVEILEDER_VEILEDER_API, VEILEDER_ID))
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body(newJsonBody(body -> {
                        body.stringType("ident")
                            .stringType("navn")
                            .stringType("fornavn")
                            .stringType("etternavn");
                    }).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactVeilederInfoFinnes")
    void testPactVeilederInfoFinnes(MockServer mockServer) throws IOException {
        int responseStatus = RestUtils.withClient(client -> client
                .target(format("%s/%s", mockServer.getUrl() + VEILARBVEILEDER_VEILEDER_API, VEILEDER_ID))
                .request()
                .get()
                .getStatus()
        );
        assertThat(responseStatus, is(equalTo(200)));
    }

    @Pact(provider = VEILARB_VEILEDER, consumer = VEILARB_PORTEFOLJE)
    public RequestResponsePact createPactVeilederInfoEnhetFinnes(PactDslWithProvider builder) {
        return builder
                .given("a request for info about a veileder in enhet")
                .uponReceiving("request about an existing veileder in enhet")
                    .matchPath(VEILARBVEILEDER_ENHET_API + "/\\d+/identer", format("%s/%s/%s", VEILARBVEILEDER_ENHET_API, VEILARB_ENHET, IDENTER))
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body(PactDslJsonArray.arrayMinLike(1, PactDslJsonRootValue.stringType(VEILEDER_ID)))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactVeilederInfoEnhetFinnes")
    void testPactVeilederInfoEnhetFinnes(MockServer mockServer) throws IOException {
        EnvironmentUtils.setProperty("veilarbveileder.api.url",  mockServer.getUrl() + "/veilarbveileder/api", PUBLIC);
        VeilederService veilederService = new VeilederService(RestUtils.createClient());
        final List<VeilederId> identer = veilederService.getIdenter(VEILARB_ENHET);
        assertThat(identer, is(not(identer.isEmpty())));
    }

    @Pact(provider = VEILARB_VEILEDER, consumer = VEILARB_PORTEFOLJE)
    public RequestResponsePact createPactVeilederInfoFinnesIkke(PactDslWithProvider builder) {
        return builder
                .given("a request for info about an unknown veileder")
                .uponReceiving("request about an unknown veileder")
                    .matchPath(VEILARBVEILEDER_VEILEDER_API + "/\\w+", VEILARBVEILEDER_VEILEDER_API + "/unknown")
                    .method("GET")
                .willRespondWith()
                    .status(204)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactVeilederInfoFinnesIkke")
    void testPactVeilederInfoFinnesIkke(MockServer mockServer) throws IOException {
        int responseStatus = RestUtils.withClient(client -> client
                .target(mockServer.getUrl() + VEILARBVEILEDER_VEILEDER_API + "/unknown")
                .request()
                .get()
                .getStatus()
        );

        assertThat(responseStatus, is(equalTo(204)));
    }
}
