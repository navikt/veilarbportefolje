package no.nav.fo.config;


import net.sf.ehcache.config.CacheConfiguration;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    public static final String tilgangTilEnhetCache = "tilgangTilEnhetCache";

    @Bean
    public net.sf.ehcache.CacheManager ehCacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(createCache(tilgangTilEnhetCache, 5000));
        config.addCache(AbacContext.ABAC_CACHE);

        return net.sf.ehcache.CacheManager.newInstance(config);
    }


    @Override
    public CacheManager cacheManager() {
        return new EhCacheCacheManager(ehCacheManager());
    }

    @Override
    public CacheResolver cacheResolver() {
        return null;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new SimpleKeyGenerator();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return null;
    }

    private static CacheConfiguration createCache(String name, int maxEntries) {
        CacheConfiguration config = new CacheConfiguration(name, maxEntries);
        config.setMemoryStoreEvictionPolicy("LRU");
        config.setTimeToIdleSeconds(3600);
        config.setTimeToLiveSeconds(3600);
        return config;
    }
}
