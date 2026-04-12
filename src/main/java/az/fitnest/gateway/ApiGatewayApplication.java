package az.fitnest.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("identity-fallback", r -> r.path("/api/v1/identity/**", "/api/v1/auth/**", "/api/v1/me/**")
                        .uri("http://identity-backend:8080"))
                .route("catalog-fallback", r -> r.path("/api/v1/catalog/**", "/api/v1/gyms/**", "/api/v1/stores/**")
                        .uri("http://catalog-backend:8080"))
                .route("user-fallback", r -> r.path("/api/v1/users/**", "/api/v1/me/summary")
                        .uri("http://user-backend:8080"))
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
