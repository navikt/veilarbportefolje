package no.nav.pto.veilarbportefolje.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.featuretoggle.UnleashClientImpl;
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkClient;
import no.nav.pto.veilarbportefolje.kodeverk.KodeverkClientImpl;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

@EnableScheduling
@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@EnableAsync
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
    public UnleashClient unleashClient(EnvironmentProperties properties, AuthContextHolder authContextHolder) {
        return new UnleashClientImpl(properties.getUnleashUrl(), APPLICATION_NAME, List.of(new ByUserIdStrategy(authContextHolder)));
    }

    @Bean
    public UnleashService unleashService(UnleashClient unleashClient) {
        return new UnleashService(unleashClient);
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public KodeverkClient kodeverkClient(EnvironmentProperties environmentProperties) {
        return new KodeverkClientImpl(environmentProperties.getKodeverkUrl());
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
