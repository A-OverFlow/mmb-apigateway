package com.mumulbo.gateway.filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    @Bean
    public KeyResolver customKeyResolver() {
        final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);
        return exchange -> {
            // 로그인 여부 확인
            String id = exchange.getRequest().getHeaders().getFirst("X-User-Id");

            if (id != null && !id.isBlank()) {
                // 로그인된 사용자: ID 기반
                log.debug("Block id {}", id);
                return Mono.just("user:" + id);
            } else {
                // 로그인되지 않은 사용자: IP 기반
                String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                log.debug("Block ip {}", ip);
                return Mono.just("ip:" + ip);
            }
        };
    }
}
