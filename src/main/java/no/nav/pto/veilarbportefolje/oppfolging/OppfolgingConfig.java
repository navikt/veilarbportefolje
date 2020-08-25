package no.nav.pto.veilarbportefolje.oppfolging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OppfolgingConfig {
    @Bean
    public OppfolgingRepository oppfolgingRepository(JdbcTemplate db){
        return new OppfolgingRepository(db);
    }
}
