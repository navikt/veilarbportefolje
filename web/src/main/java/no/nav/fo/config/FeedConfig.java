package no.nav.fo.config;

import no.nav.fo.consumer.feed.FeedClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeedConfig {
    @Bean
    public FeedClient feedClient() {
        return new FeedClient();
    }
}
