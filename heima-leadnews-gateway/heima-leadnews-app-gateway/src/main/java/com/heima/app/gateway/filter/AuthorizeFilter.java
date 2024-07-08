package com.heima.app.gateway.filter;


import com.heima.app.gateway.utils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        //判断是否为登录操作
        if(request.getURI().getPath().contains("/login")){
            //放行
            return chain.filter(exchange);
        }

        //判断token是否合法
        String token = request.getHeaders().getFirst("token");
        //token不存在，返回401
        if(StringUtils.isEmpty(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //token过期，返回401
        Claims claimsBody = AppJwtUtil.getClaimsBody(token);
        int result = AppJwtUtil.verifyToken(claimsBody);
        if(result == 1|| result ==2){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //获取用户信息
        Object id = claimsBody.get("id");
        ServerHttpRequest serverHttpRequest = request.mutate().headers(httpHeaders -> {
            httpHeaders.add("userId", id + "");
        }).build();

        exchange.mutate().request(serverHttpRequest);

        //放行
        return chain.filter(exchange);
        
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
