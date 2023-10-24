package no.nav.pto.veilarbportefolje.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import io.getunleash.DefaultUnleash;
import io.getunleash.util.UnleashConfig;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkClient;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkClientImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@EnableScheduling
@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationConfig {

    public static final String APPLICATION_NAME = "veilarbportefolje";

    @Bean
    public TaskScheduler taskScheduler() {
        ConcurrentTaskScheduler scheduler = new ConcurrentTaskScheduler();
        scheduler.setErrorHandler(new ScheduledErrorHandler());
        return scheduler;
    }

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }

    @Bean
    public AzureAdMachineToMachineTokenClient azureAdMachineToMachineTokenClient() {
        return AzureAdTokenClientBuilder.builder()
                .withNaisDefaults()
                .buildMachineToMachineTokenClient();
    }

    @Bean
    public DefaultUnleash defaultUnleash(EnvironmentProperties properties) {
        String environment = EnvironmentUtils.isProduction().orElse(false) ? "production" : "development";
        UnleashConfig config = UnleashConfig.builder()
                .appName(APPLICATION_NAME)
                .instanceId(APPLICATION_NAME)
                .unleashAPI(properties.getUnleashUrl())
                .apiKey(properties.getUnleashApiToken())
                .environment(environment)
                .build();
        return new DefaultUnleash(config);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public KodeverkClient kodeverkClient(EnvironmentProperties environmentProperties) {
        return new KodeverkClientImpl(environmentProperties.getKodeverkUrl());
    }

}
