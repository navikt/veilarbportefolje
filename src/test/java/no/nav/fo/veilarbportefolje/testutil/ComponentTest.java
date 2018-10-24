package no.nav.fo.veilarbportefolje.testutil;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import lombok.SneakyThrows;
import no.nav.apiapp.ApiApp;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbportefolje.config.ComponentTestConfig;
import no.nav.fo.veilarbportefolje.config.DatabaseConfig;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import no.nav.testconfig.ApiAppTest;
import org.eclipse.jetty.plus.jndi.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.servlet.*;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.*;

import static com.squareup.okhttp.MediaType.parse;
import static java.lang.System.setProperty;
import static javax.ws.rs.core.HttpHeaders.COOKIE;
import static no.nav.brukerdialog.security.oidc.provider.AzureADB2CProvider.AZUREADB2C_OIDC_COOKIE_NAME;
import static no.nav.common.auth.SubjectHandler.withSubject;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.testconfig.ApiAppTest.setupTestContext;

public abstract class ComponentTest {
    private static final String CONTEXT_NAME = "veilarbportefolje";
    private static final int PORT = tilfeldigPort();
    //    private static final Jetty JETTY = nyJetty(CONTEXT_NAME, tilfeldigPort());
    private static final OkHttpClient OKHTTPCLIENT = new OkHttpClient();
    protected static SingleConnectionDataSource ds;

    private static final Subject TEST_SUBJECT = new Subject("testident", IdentType.InternBruker, SsoToken.oidcToken("token"));
    private static Jetty jetty;

    @BeforeClass
    public static void startJetty() {
        SubjectHandler.withSubject(new Subject("testident", IdentType.InternBruker, SsoToken.oidcToken("token")), () -> {
            setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
            setupDataSource();
            ApiApp apiApp = ApiApp.startApiApp(ComponentTestConfig.class, new String[]{String.valueOf(PORT)});
            jetty = apiApp.getJetty();
        });
    }

    @AfterClass
    public static void stopJetty() {
        jetty.stop.run();
//        JETTY.stop.run();
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
    protected Response delete(String path, String token) {
        Request request = new Request.Builder()
                .url(getUrl(path))
                .header(COOKIE, AZUREADB2C_OIDC_COOKIE_NAME + "=" + token)
                .delete()
                .build();
        return OKHTTPCLIENT.newCall(request).execute();
    }

    private RequestBody createBody(String json) {
        return RequestBody.create(parse("application/json; charset=utf-8"), json);
    }

    private URL getUrl(String path) throws MalformedURLException {
        URL url = uri(path).toURL();
        System.out.println(url.toString());
        return url;
    }

    private static URI uri(String path) {
        return UriBuilder.fromPath(CONTEXT_NAME + path).host(getHostName()).scheme("http").port(getPort()).build();
    }

    private static int getPort() {
        return PORT;
//        return ((ServerConnector) JETTY.server.getConnectors()[0]).getPort();
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

//    private static Jetty nyJetty(String contextPath, int jettyPort) {
//        setupProperties();
//        setupDataSource();
//
//        Jetty jetty = Jetty.usingWar()
//                .at(contextPath)
//                .port(jettyPort)
//                .overrideWebXml(new File("src/test/resources/componenttest-web.xml"))
//                .disableAnnotationScanning()
//                .addFilter(new TestSubjectFilter())
//                .buildJetty();
//
//        // MetaInfConfiguration førte til "java.util.zip.ZipException: error in opening zip file"
//        WebAppContext context = jetty.context;
//        String[] configurations = stream(context.getConfigurationClasses())
//                .filter(className -> !MetaInfConfiguration.class.getName().equals(className))
//                .toArray(String[]::new);
//        context.setConfigurationClasses(configurations);
//
//        return jetty;
//    }

    @SneakyThrows
    private static void setupDataSource() {
        ds = setupInMemoryDatabase();
        new Resource(DatabaseConfig.JNDI_NAME, ds);
    }

    private static void setupProperties() {
        setProperty("APP_NAME", APPLICATION_NAME);
        System.setProperty("APP_LOG_HOME", new File("target").getAbsolutePath());
        System.setProperty("application.name", APPLICATION_NAME);
    }

    private static class TestSubjectFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            withSubject(TEST_SUBJECT, () -> {
                chain.doFilter(request, response);
            });
        }

        @Override
        public void destroy() {

        }
    }
}
