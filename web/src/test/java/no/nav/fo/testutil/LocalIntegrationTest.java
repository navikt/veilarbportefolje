package no.nav.fo.testutil;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.sbl.dialogarena.test.SystemProperties;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.*;

import static com.squareup.okhttp.MediaType.parse;
import static java.lang.System.setProperty;
import static no.nav.fo.StartJettyVeilArbPortefolje.APPLICATION_NAME;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;


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
    static {
        setProperty("testmiljo", "t6");
    }

    private static final String CONTEXT_NAME = LocalIntegrationTest.class.getSimpleName();
    private static final Jetty JETTY = nyJetty(CONTEXT_NAME, tilfeldigPort());
    private static final OkHttpClient OKHTTPCLIENT = new OkHttpClient();
    protected static SingleConnectionDataSource ds;

    @BeforeClass
    public static void startJetty() {
        JETTY.start();
    }

    @AfterClass
    public static void stopJetty() {
        JETTY.stop.run();
    }

    @Before
    public void setUp() throws Exception {
        setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
        InternbrukerSubjectHandler.setVeilederIdent("testident");
        System.clearProperty("portefolje.pilot.enhetliste");
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

    private static Jetty nyJetty(String contextPath, int jettyPort) {
        setupProperties();
        setupDataSource();

        Jetty.JettyBuilder builder = Jetty.usingWar()
                .at(contextPath)
                .port(jettyPort)
                .overrideWebXml(new File("src/test/resources/localintegration-web.xml"))
                .disableAnnotationScanning();

        DevelopmentSecurity.ISSOSecurityConfig issoSecurityConfig =
                new DevelopmentSecurity.ISSOSecurityConfig(APPLICATION_NAME);

        return DevelopmentSecurity
                .setupISSO(builder, issoSecurityConfig)
                .configureForJaspic()
                .buildJetty();
    }

    @SneakyThrows
    private static void setupDataSource() {
        ds = setupInMemoryDatabase();
        new Resource(DatabaseConfig.JNDI_NAME, ds);
    }

    private static void setupProperties() {
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", APPLICATION_NAME);
        SystemProperties.setFrom("veilarbportefolje.properties");
    }

}
