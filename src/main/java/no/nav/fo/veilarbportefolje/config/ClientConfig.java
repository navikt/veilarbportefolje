package no.nav.fo.veilarbportefolje.config;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
import net.jodah.failsafe.event.ExecutionCompletedEvent;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.fo.veilarbportefolje.FailSafeConfig;
import no.nav.fo.veilarbportefolje.krr.ErrorDTO;
import no.nav.sbl.rest.RestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.Function;

import static java.time.Duration.ofSeconds;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.sbl.rest.RestUtils.DEFAULT_CONFIG;

@Slf4j
@Configuration
public class ClientConfig {

    private static FailSafeConfig defaultFailsafeConfig = FailSafeConfig.builder()
            .maxRetries(3)
            .retryDelay(ofSeconds(8))
            .timeout(ofSeconds(30))
            .build();

    public static <T> T usingFailSafeClient(FailSafeConfig config, Function<Client, T> function) {

        RetryPolicy<T> retryPolicy = new RetryPolicy<T>()
                .handle(IOException.class)
                .withDelay(config.getRetryDelay())
                .withMaxRetries(config.getMaxRetries());

        Timeout<T> timeout = Timeout.of(config.getTimeout());

        return Failsafe
                .with(retryPolicy, timeout)
                .onFailure(ClientConfig::logFailure)
                .onSuccess(success -> log.info("Call succeeded after {} attempt(s)", success.getAttemptCount()))
                .get(() -> RestUtils.withClient(function));
    }

    private static <T> void logFailure(ExecutionCompletedEvent<T> e) {
        log.error("\nFailure: {} \n Message: {} \n Stacktrace: {} \n Result: {}", e.getFailure(), e.getFailure().getMessage(), e.getFailure().getStackTrace(), e.getResult());
    }

    public static <T> T usingFailSafeClient(Function<Client, T> function) {
        return usingFailSafeClient(getDefaultFailsafeConfig(), function);
    }

    public static FailSafeConfig getDefaultFailsafeConfig() {
        return defaultFailsafeConfig;
    }

    public static void setDefaultFailsafeConfig(FailSafeConfig defaultFailsafeConfig) {
        ClientConfig.defaultFailsafeConfig = defaultFailsafeConfig;
    }

    @Bean
    public Client client() {
        Client client = RestUtils.createClient();
        client.register(new SystemUserOidcTokenProviderFilter());
        return client;
    }

    private static class SystemUserOidcTokenProviderFilter implements ClientRequestFilter {
        private SystemUserTokenProvider systemUserTokenProvider = new SystemUserTokenProvider();

        @Override
        public void filter(ClientRequestContext clientRequestContext) {
            clientRequestContext.getHeaders().putSingle("Authorization", "Bearer " + systemUserTokenProvider.getToken());
        }
    }

}
