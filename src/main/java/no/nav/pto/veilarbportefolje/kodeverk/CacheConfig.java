package no.nav.pto.veilarbportefolje.kodeverk;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String KODEVERK_BETYDNING_CACHE_NAME = "kodeverk_betydning_cache";


    @Bean
    public Cache kodeverkBetydningCache() {
        return new CaffeineCache(KODEVERK_BETYDNING_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(3)
                .build());
    }
}
