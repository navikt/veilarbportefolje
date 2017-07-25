package no.nav.fo.config;


import net.sf.ehcache.config.CacheConfiguration;
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

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;


@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    public static final String kode6Cache = "kode6Cache";
    public static final String kode7Cache = "kode7Cache";
    public static final String egenAnsattCache = "egenAnsattCache";
    public static final String modiaOppfolgingCache = "modiaOppfolgingCache";
    public static final String brukerTilgangCache = "brukerTilgangCache";

    private static String cacheTTLProperty = "veilarbportefolje.tilgangtilbrukercache.seconds";
    private static int cacheTimeToLive = parseInt(getProperty(cacheTTLProperty, "3600"));

    @Bean
    public net.sf.ehcache.CacheManager ehCacheManager() {
        net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
        config.addCache(createCache(kode6Cache, 4000));
        config.addCache(createCache(kode7Cache, 4000));
        config.addCache(createCache(egenAnsattCache, 4000));
        config.addCache(createCache(modiaOppfolgingCache, 10000));
        config.addCache(createCache(brukerTilgangCache, 10000));

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
        config.setTimeToLiveSeconds(cacheTimeToLive);
        return config;
    }
}
