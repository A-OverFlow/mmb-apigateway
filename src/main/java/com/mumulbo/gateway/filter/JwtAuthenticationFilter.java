package com.mumulbo.gateway.filter;

import com.mumulbo.gateway.dto.AuthResponse;
import com.mumulbo.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    WebClient webClient;
    private final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 필터 제외
        if (isPublicPath(path, exchange)) {
            log.debug("필터 제외");
            return chain.filter(exchange);
        }

        // X- header 외부 접근 차단
        for (String header : List.of("X-User-Id", "X-User-Name", "X-User-Email", "X-User-Username")) {
            if (exchange.getRequest().getHeaders().containsKey(header)) {
                System.out.println("🚨 외부 요청에 금지된 헤더 포함됨: " + header);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        // 토큰 확인
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("토큰 확인. authHeader : {}", authHeader);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 토큰 검증
        String token = authHeader.substring(7);
        return webClient.get()
            .uri("http://mmb-auth-service:8081/api/v1/auth/validate")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(AuthResponse.class)
            .flatMap(authResponse -> {
                // valid
                if (!authResponse.isValid()) {
                    log.error("유효하지 않은 토큰: {}", authResponse.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String path2 = exchange.getRequest().getPath().value();
                if (path2.startsWith("/ws/chat")) {
                    URI newUri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                        .queryParam("userId", authResponse.getId())
                        .build(true)
                        .toUri();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate().uri(newUri).build())
                        .build();

                    log.debug("WebSocket 요청 - userId 쿼리파라미터 추가됨: {}", authResponse.getId());
                    return chain.filter(mutatedExchange);
                } else {
                    ServerWebExchange finalExchange = exchange.mutate()
                        .request(builder -> builder.header("X-User-Id", authResponse.getId()))
                        .build();

                    log.debug("HTTP 요청 - X-User-Id 헤더 추가됨: {}", authResponse.getId());
                    return chain.filter(finalExchange);
                }
            })
            .onErrorResume(e -> {
                log.error("Auth API 호출 실패: {}", e.toString());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });

    }

    private boolean isPublicPath(String path, ServerWebExchange exchange) {
        String method = exchange.getRequest().getMethod().name();
        return path.startsWith("/api/v1/auth/signup") ||
                path.startsWith("/api/v1/auth/reissue") ||
                path.startsWith("/api/v1/oauth2") ||
                path.startsWith("/login/oauth2/code") ||
                path.equals("/") ||
                (path.equals("/api/v1/questions") && method.equalsIgnoreCase("GET")) ||
                (path.startsWith("/api/v1/answers") && method.equalsIgnoreCase("GET")) ||
                (path.matches("^/api/v1/questions/[^/]+/answers$") && method.equalsIgnoreCase("GET")) ||
                (path.equals("/api/v1/members/count") && method.equalsIgnoreCase("GET")) ||
                (path.equals("/api/v1/chat/messages") && method.equalsIgnoreCase("GET")) ||
                (path.equals("/api/v1/questions/count") && method.equalsIgnoreCase("GET")) ||
                path.startsWith("/static") ||
                path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".ico") ||
                path.endsWith(".html");
    }

    @Override
    public int getOrder() {
        return 3;
    }
}

