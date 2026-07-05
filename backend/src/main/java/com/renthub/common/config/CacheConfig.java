package com.renthub.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Cache configuration.
 *
 * Uses simple ConcurrentMap cache in all profiles.
 * The prod profile sets spring.cache.type=simple in application-prod.yml
 * which prevents Spring from trying to auto-configure Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
                "equipment",
                "categories",
                "popularEquipment",
                "userSessions",
                "dashboardData"
        );
    }
}
