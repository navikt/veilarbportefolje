package no.nav.sbl.fo.config;

import no.nav.sbl.fo.internal.HealthCheckService;
import no.nav.sbl.fo.internal.IsAliveServlet;

//@Configuration
//@Import({
//        Pingables.class,
//        DatabaseConfig.class
//})
//@ImportResource({"classpath:spring-security.xml", "classpath:spring-security-web.xml"})
//public class ApplicationConfig {
//
//    @Bean
//    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
//        return new PropertySourcesPlaceholderConfigurer();
//    }
//
//    @Bean
//    public IsAliveServlet isAliveServlet() {
//        return new IsAliveServlet();
//    }
//
//    @Bean
//    public HealthCheckService healthCheckService() {
//        return new HealthCheckService();
//    }
//}