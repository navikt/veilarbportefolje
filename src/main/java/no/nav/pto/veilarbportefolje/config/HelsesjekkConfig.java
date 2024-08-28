package no.nav.pto.veilarbportefolje.config;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchHealthCheck;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.pto.veilarbportefolje.opensearch.OpensearchHealthCheck.FORVENTET_MINIMUM_ANTALL_DOKUMENTER;

@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(AktorClient aktorClient,
                                          JdbcTemplate jdbcTemplate,
                                         OpensearchHealthCheck opensearchHealthCheck) {
        List<SelfTestCheck> asyncSelftester = List.of(
                new SelfTestCheck(String.format("Sjekker at antall dokumenter > %s", FORVENTET_MINIMUM_ANTALL_DOKUMENTER), false, opensearchHealthCheck),
                new SelfTestCheck("Database for portefolje", true, () -> dbPinger(jdbcTemplate)),
                new SelfTestCheck("Aktorregister", true, aktorClient)
        );
        return new SelfTestChecks(asyncSelftester);
    }

    @Bean
    public SelfTestMeterBinder selfTestMeterBinder(SelfTestChecks selfTestChecks) {
        return new SelfTestMeterBinder(selfTestChecks);
    }

    public static HealthCheckResult dbPinger(JdbcTemplate db) {
        try {
            db.queryForList("SELECT 1");
            return HealthCheckResult.healthy();
        } catch (Exception e) {
            return HealthCheckResult.unhealthy("Feil mot databasen", e);
        }
    }
}
