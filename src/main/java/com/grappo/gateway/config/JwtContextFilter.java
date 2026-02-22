package com.grappo.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(-1)
@Component
public class JwtContextFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        return exchange.getPrincipal()
                .filter(p -> p instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {

                    var jwt = auth.getToken();

                    String userId = jwt.getSubject();
                    String tenantId = jwt.getClaimAsString("tenant_id");
                    String dominioId = jwt.getClaimAsString("dominio_id");

                    ServerHttpRequest mutatedRequest =
                            exchange.getRequest()
                                    .mutate()
                                    .header("X-User-Id", safe(userId))
                                    .header("X-Tenant-Id", safe(tenantId))
                                    .header("X-Dominio-Id", safe(dominioId))
                                    .build();

                    return chain.filter(
                            exchange.mutate()
                                    .request(mutatedRequest)
                                    .build()
                    );
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}