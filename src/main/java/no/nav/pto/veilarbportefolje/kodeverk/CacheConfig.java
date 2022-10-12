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
    public static final String TILGANG_TIL_MODIA_CACHE_NAME = "tilgang_til_modia_cache";
    public static final String VEDTAKSSTOTTE_PILOT_TOGGLE_CACHE_NAME = "vedtaksstotte_pilot_toggle_cache";


    @Bean
    public Cache kodeverkBetydningCache() {
        return new CaffeineCache(KODEVERK_BETYDNING_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(3)
                .build());
    }

    @Bean
    public Cache tilgangTilModiaCache() {
        return new CaffeineCache(TILGANG_TIL_MODIA_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10_000)
                .build());
    }

    @Bean
    public Cache vedtakstottePilotToggleCache() {
        return new CaffeineCache(VEDTAKSSTOTTE_PILOT_TOGGLE_CACHE_NAME, Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build());
    }
}
