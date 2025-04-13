package com.mumulbo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

  private final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    StringBuilder sb = new StringBuilder();
    sb.append("\n\n🔷🔷🔷 [GATEWAY REQUEST START] 🔷🔷🔷\n");
    sb.append("▶ Method      : ").append(exchange.getRequest().getMethod()).append("\n");
    sb.append("▶ URI         : ").append(exchange.getRequest().getURI()).append("\n");
    sb.append("▶ Remote Addr : ").append(exchange.getRequest().getRemoteAddress()).append("\n");
    sb.append("▶ Headers     : ").append(exchange.getRequest().getHeaders()).append("\n");
    sb.append("▶ Query Params: ").append(exchange.getRequest().getQueryParams()).append("\n");

    log.debug(sb.toString());

    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return -1;//가장 앞단에서 실행되어 요청 정보 캡처용으로 최적
  }
}