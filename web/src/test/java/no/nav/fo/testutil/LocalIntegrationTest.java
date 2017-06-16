package no.nav.fo.testutil;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import lombok.SneakyThrows;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.*;

import static com.squareup.okhttp.MediaType.parse;


/**
 * Bruk denne klassen for å skrive integrajonstester som går mot en lokal instans av applikasjonen.
 * Denne klassen vil starte en lokal Jetty-server samt en in-memory database.
 *
 *                                    !!OBS!!
 *    ==============================================================================
 *   |        Unngå å gjør kall over nettverk! MOCK UT ALLE NETTVERKSKALL!         |
 *   ===============================================================================
 *                                    !!OBS!!
 *
 * Eksempel:
 *
 * public class MinLocalIntegrationTest extends LocalIntegrationTest {
 *
 *     @Test
 *     public void skalReturnereOk() throws Exception {
 *         Response response = get("/tjenester/arbeidsliste/12345678900");
 *         assertEquals(200, response.code());
 *     }
 * }
 *
 */
public abstract class LocalIntegrationTest {

    private static final String CONTEXT_NAME = LocalIntegrationTest.class.getSimpleName();
    private static final Jetty JETTY = StartJetty.nyJetty(CONTEXT_NAME, tilfeldigPort());
    private static final OkHttpClient OKHTTPCLIENT = new OkHttpClient();

    @BeforeClass
    public static void startJetty() {
        JETTY.start();
    }

    @AfterClass
    public static void stopJetty() {
        JETTY.stop.run();
    }

    @SneakyThrows
    protected Response get(String path) {
        Request request = new Request.Builder()
                .url(getUrl(path))
                .get()
                .build();
        return OKHTTPCLIENT.newCall(request).execute();
    }

    @SneakyThrows
    protected Response put(String path, String json) {
        RequestBody body = createBody(json);
        Request request = new Request.Builder()
                .url(getUrl(path))
                .put(body)
                .build();
        return OKHTTPCLIENT.newCall(request).execute();
    }

    @SneakyThrows
    protected Response post(String path, String json) {
        Request request = new Request.Builder()
                .url(getUrl(path))
                .post(createBody(json))
                .build();
        return OKHTTPCLIENT.newCall(request).execute();
    }

    @SneakyThrows
    protected Response delete(String path) {
        Request request = new Request.Builder()
                .url(getUrl(path))
                .delete()
                .build();
        return OKHTTPCLIENT.newCall(request).execute();
    }

    private RequestBody createBody(String json) {
        return RequestBody.create(parse("application/json; charset=utf-8"), json);
    }

    private URL getUrl(String path) throws MalformedURLException {
        return uri(path).toURL();
    }

    private static URI uri(String path) {
        return UriBuilder.fromPath(CONTEXT_NAME + path).host(getHostName()).scheme("http").port(getPort()).build();
    }

    private static int getPort() {
        return ((ServerConnector) JETTY.server.getConnectors()[0]).getPort();
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static int tilfeldigPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
