package no.nav.fo.pacts;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static io.pactfoundation.consumer.dsl.LambdaDsl.newJsonBody;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@ExtendWith(PactConsumerTestExt.class)
public class PactVeilederInfoConsumerTest {

    private static final String VEILARB_PORTEFOLJE = "veilarbportefolje";
    private static final String VEILARB_VEILEDER = "veilarbveileder";
    private static final String VEILARBVEILEDER_VEILEDER_API = "/veilarbveileder/veileder";

    @Pact(provider=VEILARB_VEILEDER, consumer=VEILARB_PORTEFOLJE)
    public RequestResponsePact createPactVeilederInfoFinnes(PactDslWithProvider builder) {
        return builder
                .given("a request for info about a veileder")
                .uponReceiving("request about an existing veileder")
                    .matchPath(VEILARBVEILEDER_VEILEDER_API + "/\\w{7}", VEILARBVEILEDER_VEILEDER_API + "/AB12345")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .body(newJsonBody(body -> {
                        body.stringType("ident");
                        body.stringType("navn");
                        body.stringType("fornavn");
                        body.stringType("etternavn");
                    }).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createPactVeilederInfoFinnes")
    void testPactVeilederInfoFinnes(MockServer mockServer) throws IOException {
        HttpResponse httpResponse = Request.Get(mockServer.getUrl() + VEILARBVEILEDER_VEILEDER_API + "/AB12345").execute().returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(200)));
    }

    @Pact(provider=VEILARB_VEILEDER, consumer=VEILARB_PORTEFOLJE)
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
        HttpResponse httpResponse = Request.Get(mockServer.getUrl() + VEILARBVEILEDER_VEILEDER_API + "/test").execute().returnResponse();
        assertThat(httpResponse.getStatusLine().getStatusCode(), is(equalTo(204)));
    }
}
