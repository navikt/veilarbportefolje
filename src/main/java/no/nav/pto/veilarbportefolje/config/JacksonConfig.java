package no.nav.pto.veilarbportefolje.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public JsonMapper objectMapper() {
        return (JsonMapper) no.nav.common.json.JsonMapper.defaultObjectMapper();
    }
}
