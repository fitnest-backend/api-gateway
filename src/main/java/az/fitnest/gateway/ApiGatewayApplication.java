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
                // ========== Business Routes ==========
                .route("identity-backend", r -> r.path("/api/v1/identity/**", "/api/v1/auth/**", "/api/v1/legal/**", "/api/v1/admin/legal/**", "/api/v1/admin/users/**", "/api/v1/admin/roles/**", "/api/v1/internal/users/**", "/api/v1/me/**")
                        .uri("http://identity-backend:8080"))
                .route("catalog-backend", r -> r.path("/api/v1/admin/stores/**", "/api/v1/admin/gyms/**", "/api/v1/admin/translations/**", "/api/v1/admin/categories/**", "/api/v1/categories/**", "/api/v1/gyms/**", "/api/v1/stores/**", "/api/v1/media/stream/**")
                        .uri("http://catalog-backend:8080"))
                .route("notifications-backend", r -> r.path("/api/v1/notifications/**", "/api/v1/devices/**", "/api/v1/sms/**", "/api/v1/admin/notifications/**", "/api/v1/admin/devices/**")
                        .uri("http://notifications-backend:8080"))
                .route("payment-backend", r -> r.path("/api/v1/me/payments/**", "/api/v1/me/cards/**", "/api/heartbeat", "/payment/**")
                        .uri("http://payment-backend:8080"))
                .route("support-backend", r -> r.path("/api/v1/admin/faqs/**", "/api/v1/admin/faq-categories/**", "/api/v1/admin/support/**", "/api/v1/support/**", "/api/v1/faqs/**", "/api/v1/faq-categories/**", "/api/v1/tickets/**")
                        .uri("http://support-backend:8080"))
                .route("fitness-plan-backend", r -> r.path("/api/v1/nutrition-plans/**", "/api/v1/workout-plans/**")
                        .uri("http://fitness-plan-backend:8080"))
                .route("order-backend", r -> r.path("/api/v1/subscription-packages/**", "/api/v1/me/subscriptions/**", "/api/v1/subscriptions/**", "/api/v1/orders/**", "/api/v1/admin/subscriptions/**", "/api/v1/admin/plans/**", "/api/v1/admin/subscription-packages/**", "/admin/subscription/**")
                        .uri("http://order-backend:8080"))
                .route("user-backend", r -> r.path("/api/v1/admin/goals/**", "/api/v1/bmi/**", "/api/v1/recent-searches/**", "/api/v1/goals/**", "/api/v1/languages/**", "/api/v1/translations/**", "/api/v1/me/summary", "/api/v1/me/**")
                        .uri("http://user-backend:8080"))
                .route("argocd", r -> r.path("/api/v1/applications/**", "/api/v1/stream/applications/**")
                        .uri("http://argocd-server.argocd.svc:8080"))

                // ========== OpenAPI Routes ==========
                .route("identity-openapi", r -> r.path("/v3/api-docs/identity-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://identity-backend:8080"))
                .route("catalog-openapi", r -> r.path("/v3/api-docs/catalog-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://catalog-backend:8080"))
                .route("notifications-openapi", r -> r.path("/v3/api-docs/notifications-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://notifications-backend:8080"))
                .route("payment-openapi", r -> r.path("/v3/api-docs/payment-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://payment-backend:8080"))
                .route("support-openapi", r -> r.path("/v3/api-docs/support-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://support-backend:8080"))
                .route("user-openapi", r -> r.path("/v3/api-docs/user-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://user-backend:8080"))
                .route("fitness-plan-openapi", r -> r.path("/v3/api-docs/fitness-plan-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://fitness-plan-backend:8080"))
                .route("order-openapi", r -> r.path("/v3/api-docs/order-backend")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri("http://order-backend:8080"))
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
