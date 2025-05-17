package com.mumulbo.gateway.filter;

import com.mumulbo.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // 필터 제외
        if (isPublicPath(path)) {
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

        String token = authHeader.substring(7);

        try {
            // todo : 직접 JWT 검증하는 게 아니라 auth api 호출해서 검증해야 함
            //  JWT_SECRET_KEY 를 직접 관리할 필요 없음. auth 만 알고 있으면 됨
            //  x-user-id로 설정한 값을 auth api의 응답으로 받으면 됨
            Claims claims = jwtUtil.validateToken(token);
            String userId = claims.getSubject(); // sub 값 추출

            // 기존 요청에 헤더 추가 (mutate 사용)
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(builder -> builder.header("X-User-Id", userId))
                    .build();

            log.debug("set x-user-id : {}", userId);
            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            log.error(e.toString());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/signup") ||
                path.startsWith("/api/v1/auth/reissue") ||
                path.startsWith("/api/v1/oauth2") ||
                path.startsWith("/login/oauth2/code") ||
                path.startsWith("/grafana") ||
                path.startsWith("/chat") ||
                path.startsWith("/ws/chat") ||
                path.equals("/") ||
                path.equals("/api/v1/questions") || // todo 로그인안해도 질문 리스트를 볼수 있어야 하니까. 정확하게 GET 요청만 허용하면 좋을텐데?
                path.startsWith("/static") ||
                path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".ico") ||
                path.endsWith(".html");
    }

    @Override
    public int getOrder() {
        return 10;
    }
}

