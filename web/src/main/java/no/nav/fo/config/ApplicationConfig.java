package no.nav.fo.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.ServletUtil;
import no.nav.fo.filmottak.FilmottakConfig;
import no.nav.fo.internal.PingConfig;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.PepClientImpl;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static no.nav.apiapp.ApiApplication.Sone.FSS;

@EnableScheduling
@EnableAspectJAutoProxy
@Configuration
@Import({
        AbacContext.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class,
        ExternalServiceConfig.class,
        SolrConfig.class,
        AktoerEndpointConfig.class,
        FilmottakConfig.class,
        MetricsConfig.class,
        AktoerEndpointConfig.class,
        CacheConfig.class,
        PingConfig.class,
        FeedConfig.class,
        RestConfig.class

})
public class ApplicationConfig implements ApiApplication {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        return new JtaTransactionManager();
    }

    @Bean
    public OppdaterBrukerdataFletter tilordneVeilederFletter() {
        return new OppdaterBrukerdataFletter();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClientImpl(pep);
    }

    @Override
    public Sone getSone() {
        return FSS;
    }

    @Bean
    public HovedindekseringScheduler hovedindekseringScheduler() {
        return new HovedindekseringScheduler();
    }

    @Override
    public void startup(ServletContext servletContext) {
        forwardTjenesterTilApi(servletContext);
    }

    // Bakoverkompatibilitet for klienter som går mot /tjenester
    public static void forwardTjenesterTilApi(ServletContext servletContext) {
        GenericServlet genericServlet = new GenericServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
                HttpServletRequest request = (HttpServletRequest) req;
                String requestURI = request.getRequestURI();
                String relativPath = requestURI.substring(request.getContextPath().length() + request.getServletPath().length() + 1);
                String apiPath = DEFAULT_API_PATH + relativPath;
                logger.warn("bakoverkompatibilitet: {} -> {}", requestURI, apiPath);
                RequestDispatcher requestDispatcher = req.getRequestDispatcher(apiPath);
                requestDispatcher.forward(req, res);
                System.out.println("!");
            }
        };
        ServletUtil.leggTilServlet(servletContext, genericServlet, "/tjenester/*");
    }

}
