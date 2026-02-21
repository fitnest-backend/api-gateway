package az.fitnest.gateway.web;

import az.fitnest.gateway.service.JwtProcessor;
import az.fitnest.gateway.service.StoreGatewayService;
import az.fitnest.gateway.web.support.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Stores (Gateway)", description = "Unified endpoints for marketplace stores, proxied to Marketplace Service via gRPC.")
public class StoreProxyController {

    private final StoreGatewayService storeService;
    private final JwtProcessor jwtProcessor;

    // --- Public & User Endpoints ---

    @GetMapping("/stores")
    @Operation(summary = "Get marketplace stores", description = "Returns a paginated list of stores with optional search.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stores retrieved successfully", content = @Content(mediaType = "application/json"))
    })
    public Mono<ResponseEntity<String>> getMarketStores(
            ServerWebExchange exchange, 
            @Parameter(description = "Search query") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Page index (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int pageSize) {
        return extractUserId(exchange)
                .map(userId -> storeService.getMarketStores(q, page, pageSize, userId))
                .map(json -> ResponseEntity.ok().header("Content-Type", "application/json").body(json));
    }

    @PostMapping("/stores/{storeId}/favorite")
    @Operation(summary = "Toggle favorite store", description = "Toggles the 'favorite' status of a store for the current user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Toggle successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Mono<ResponseEntity<String>> toggleFavorite(@PathVariable String storeId, ServerWebExchange exchange) {
        return extractUserId(exchange)
                .flatMap(userId -> {
                    if (userId == 0L) return Mono.just(ResponseEntity.status(401).body("{\"error\":\"Unauthorized\"}"));
                    storeService.toggleFavorite(storeId, userId);
                    return Mono.just(ResponseEntity.ok().body("{\"success\":true}"));
                });
    }

    // --- Admin Endpoints ---

    @PostMapping("/admin/stores")
    @Operation(summary = "Create store (Admin)", description = "Creates a new store. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Store created successfully") })
    public Mono<ResponseEntity<String>> createStoreAdmin(@RequestBody String body) {
        return Mono.fromCallable(() -> {
            String json = storeService.createStoreAdmin(body);
            return ResponseEntity.status(201).header("Content-Type", "application/json").body(json);
        });
    }

    @PutMapping("/admin/stores/{storeId}")
    @Operation(summary = "Update store (Admin)", description = "Updates an existing store. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<String>> updateStoreAdmin(@PathVariable String storeId, @RequestBody String body) {
        return Mono.fromCallable(() -> {
            String json = storeService.updateStoreAdmin(storeId, body);
            return ResponseEntity.ok().header("Content-Type", "application/json").body(json);
        });
    }

    @DeleteMapping("/admin/stores/{storeId}")
    @Operation(summary = "Delete store (Admin)", description = "Deletes a store. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> deleteStoreAdmin(@PathVariable String storeId) {
        return Mono.fromCallable(() -> {
            storeService.deleteStoreAdmin(storeId);
            return ResponseEntity.noContent().build();
        });
    }

    private Mono<Long> extractUserId(ServerWebExchange exchange) {
        String token = RequestUtils.extractToken(exchange);
        if (token == null || token.isBlank()) {
            return Mono.just(0L); // Anonymous
        }
        return jwtProcessor.validateTokenForGateway(token)
                .map(validation -> {
                    if (validation.valid && validation.userId != null) {
                        return validation.userId;
                    }
                    return 0L;
                })
                .onErrorReturn(0L);
    }
}
