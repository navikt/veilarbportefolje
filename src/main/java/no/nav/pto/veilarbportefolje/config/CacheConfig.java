package no.nav.pto.veilarbportefolje.config;


import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static net.sf.ehcache.store.MemoryStoreEvictionPolicy.LRU;


@Configuration
@EnableCaching
public class CacheConfig {
    public static final String TILGANG_TIL_ENHET = "tilgangTilEnhet";
    private static final CacheConfiguration TILGANG_TIL_ENHET_CACHE =
            new CacheConfiguration(TILGANG_TIL_ENHET, 5000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(3600)
                    .timeToLiveSeconds(3600);

    public static final String VEILARBVEILEDER = "veilarbveileder";
    private static final CacheConfiguration VEILARBVEILEDER_CACHE =
            new CacheConfiguration(VEILARBVEILEDER, 5000)
                    .memoryStoreEvictionPolicy(LRU)
                    .timeToIdleSeconds(3600)
                    .timeToLiveSeconds(3600);

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(TILGANG_TIL_ENHET_CACHE);
        config.addCache(ABAC_CACHE);
        config.addCache(VEILARBVEILEDER_CACHE);
        return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }

}
