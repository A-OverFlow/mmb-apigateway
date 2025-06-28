package com.mumulbo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class PostRoutingLoggingFilter implements GlobalFilter, Ordered {
  @Value("${spring.application.name:mmb-apigateway}")
  private String applicationName;

  private final Logger log = LoggerFactory.getLogger(PostRoutingLoggingFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    return Mono.deferContextual(context -> {
          MDC.put("APPLICATION_NAME", applicationName);

          log.debug("🟡 [PostRoutingLoggingFilter] 필터 호출됨");

          return chain.filter(exchange)
              .doOnError(e -> {
                  log.error("❌ 라우팅 중 오류 발생", e);
                  printRoutingLog(exchange, e);
              })
              .doOnSuccess(v -> {
                  printRoutingLog(exchange, null);
              })
            .doFinally(signalType -> MDC.remove("APPLICATION_NAME"));
        });
  }


  private void printRoutingLog(ServerWebExchange exchange, Throwable ex) {
    URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    Object routeAttr = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

    String routeId = null;
    if (routeAttr instanceof org.springframework.cloud.gateway.route.Route) {
      routeId = ((org.springframework.cloud.gateway.route.Route) routeAttr).getId();
    }

    String method = exchange.getRequest().getMethod() != null
        ? exchange.getRequest().getMethod().name()
        : "UNKNOWN";

    // 🔸 요청자가 보낸 원래 URI (rewrite 전)
    String originalRequestUri = exchange.getRequest().getURI().toString();

    // 🔸 RewritePath 적용된 최종 URI (gateway 내부 전송 대상)
    String rewrittenPath = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR) != null
        ? exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR).toString()
        : "❗ 최종 URI 없음";

    String result = ex != null ? "❌ FAILURE: " + ex.getMessage() : "✅ SUCCESS";

    log.warn("\n🚨 [GATEWAY ROUTING RESULT]\n" +
            "🔸 요청 메서드     : {}\n" +
            "🔸 원래 요청 URI    : {}\n" +
            "🔸 변환된 요청 URI  : {}\n" +
            "🔸 라우팅 대상 URI  : {}\n" +
            "🔸 라우트 ID        : {}\n" +
            "🔸 결과             : {}\n",
        method,
        originalRequestUri,
        rewrittenPath,
        targetUri != null ? targetUri : "❗ Target URI 없음",
        routeId != null ? routeId : "❗ Route ID 없음",
        result);
  }


  @Override
  public int getOrder() {
    return 10100; // RouteToRequestUrlFilter 이후 실행되어 targetUri 로깅 확실하게 가능
  }
}