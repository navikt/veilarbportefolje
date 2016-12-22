package no.nav.sbl.fo.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public VirksomhetEnhetServiceImpl virksomhetEnhetServiceImpl() {
        return new VirksomhetEnhetServiceImpl();
    }
}
