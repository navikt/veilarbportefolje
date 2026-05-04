package no.nav.pto.veilarbportefolje.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configures a Jackson 2.x (com.fasterxml) ObjectMapper as the primary Spring bean.
 * Required because nav-common types (Fnr, AktorId, etc.) use Jackson 2.x annotations
 * (@JsonCreator, @JsonProperty), which are not recognized by Jackson 3.x.
 * Spring Boot 4.0 defaults to Jackson 3.x; this bean overrides that so that
 * HttpMessageConvertersAutoConfiguration registers a MappingJackson2HttpMessageConverter.
 * findAndRegisterModules() auto-discovers all modules on classpath, including
 * KotlinModule (needed for Kotlin data classes) and JavaTimeModule.
 * See also: spring.http.converters.preferred-json-mapper=jackson2 in application.properties
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
