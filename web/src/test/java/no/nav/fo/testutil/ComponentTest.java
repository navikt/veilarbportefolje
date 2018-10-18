package no.nav.fo.testutil;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import lombok.SneakyThrows;
import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.fo.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
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
import static java.util.Arrays.stream;
import static no.nav.fo.StartJettyVeilArbPortefolje.APPLICATION_NAME;
import static no.nav.fo.config.LocalJndiContextConfig.setupInMemoryDatabase;

public abstract class ComponentTest {
    private static final String CONTEXT_NAME = ComponentTest.class.getSimpleName();
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

        Jetty jetty = Jetty.usingWar()
                .at(contextPath)
                .port(jettyPort)
                .overrideWebXml(new File("src/test/resources/componenttest-web.xml"))
                .disableAnnotationScanning()
                .buildJetty();

        // MetaInfConfiguration førte til "java.util.zip.ZipException: error in opening zip file"
        WebAppContext context = jetty.context;
        String[] configurations = stream(context.getConfigurationClasses())
                .filter(className -> !MetaInfConfiguration.class.getName().equals(className))
                .toArray(String[]::new);
        context.setConfigurationClasses(configurations);

        return jetty;
    }

    @SneakyThrows
    private static void setupDataSource() {
        ds = setupInMemoryDatabase();
        new Resource(DatabaseConfig.JNDI_NAME, ds);
    }

    private static void setupProperties() {
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", APPLICATION_NAME);
    }

}
