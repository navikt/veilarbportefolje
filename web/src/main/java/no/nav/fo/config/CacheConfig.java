package no.nav.fo.config;


import net.sf.ehcache.config.CacheConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext.ABAC_CACHE;


@Configuration
@EnableCaching
public class CacheConfig {

    private static final String CACHE_TTL_PROPERTY = "veilarbportefolje.tilgangtilbrukercache.seconds";
    private static final int CACHE_TIME_TO_LIVE = parseInt(getProperty(CACHE_TTL_PROPERTY, "3600"));

    public static final String TILGANG_TIL_ENHET = "tilgangTilEnhet";
    private static final CacheConfiguration TILGANG_TIL_ENHET_CACHE =
            new CacheConfiguration(TILGANG_TIL_ENHET, 5000)
                    .memoryStoreEvictionPolicy("LRU")
                    .timeToIdleSeconds(3600)
                    .timeToLiveSeconds(CACHE_TIME_TO_LIVE);

    @Bean
    public CacheManager cacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(TILGANG_TIL_ENHET_CACHE);
        config.addCache(ABAC_CACHE);
        return new EhCacheCacheManager(net.sf.ehcache.CacheManager.newInstance(config));
    }

}
