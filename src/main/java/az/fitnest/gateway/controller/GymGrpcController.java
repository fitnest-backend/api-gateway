package az.fitnest.gateway.controller;

import az.fitnest.gateway.service.GymGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Gyms (Gateway)", description = "Unified endpoints for gym discovery and management, proxied to Marketplace Service via gRPC.")
public class GymGrpcController {

    private final GymGatewayService gymService;

    // --- Public & User Endpoints ---

    @GetMapping("/gyms/{gymId}")
    @Operation(summary = "Get gym details", description = "Retrieves full details of a specific gym, including location and facilities.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Gym details retrieved successfuly"),
            @ApiResponse(responseCode = "404", description = "Gym not found")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getGymDetail(
            @Parameter(description = "Authenticated User ID (Internal header)") @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @Parameter(description = "ID of the gym") @PathVariable Long gymId) {
        return Mono.fromCallable(() -> {
            Long userId = (userIdStr != null && !userIdStr.isEmpty()) ? Long.parseLong(userIdStr) : null;
            return ResponseEntity.ok(gymService.getGymDetail(userId, gymId));
        });
    }

    @GetMapping("/gyms/{gymId}/images")
    @Operation(summary = "Get gym images", description = "Returns all images for a specific gym.")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getGymImages(@PathVariable Long gymId) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getGymImages(gymId)));
    }

    @GetMapping("/gyms/{gymId}/packages")
    @Operation(summary = "Get gym packages", description = "Returns available subscription packages for a gym.")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getGymPackages(@PathVariable Long gymId) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getGymPackages(gymId)));
    }

    @GetMapping("/gyms/{gymId}/packages/{packageId}/includes")
    @Operation(summary = "Get package inclusions", description = "Returns specific features included in a gym package.")
    public Mono<ResponseEntity<Map<String, Object>>> getPackageIncludes(
            @PathVariable Long gymId, @PathVariable Long packageId) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getPackageIncludes(gymId, packageId)));
    }

    @GetMapping("/gyms/{gymId}/trainers")
    @Operation(summary = "Get gym trainers", description = "Returns a paginated list of trainers for a gym.")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getTrainers(
            @PathVariable Long gymId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getTrainers(gymId, page, page_size)));
    }

    @GetMapping("/gyms/{gymId}/reviews")
    @Operation(summary = "Get gym reviews", description = "Returns user reviews for a gym.")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getReviews(
            @PathVariable Long gymId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size,
            @RequestParam(required = false) String sort) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getReviews(gymId, page, page_size, sort)));
    }

    @PostMapping("/gyms/{gymId}/reviews")
    @Operation(summary = "Add gym review", description = "Allows a user to submit a review for a gym.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review added successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public Mono<ResponseEntity<Void>> addReview(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long gymId,
            @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Integer rating = (Integer) body.get("rating");
            String comment = (String) body.get("comment");
            gymService.addReview(userId, gymId, rating, comment);
            return ResponseEntity.status(201).build();
        });
    }

    @GetMapping("/gyms/{gymId}/reservation-rules")
    @Operation(summary = "Get reservation rules", description = "Returns gym entry and reservation rules.")
    public Mono<ResponseEntity<Map<String, Object>>> getReservationRules(@PathVariable Long gymId) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getReservationRules(gymId)));
    }

    @GetMapping("/gyms/main-page-gyms")
    @Operation(summary = "Get gyms for main page", description = "Returns a list of gyms for the application home screen.")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getMainPageGyms(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return Mono.fromCallable(() -> ResponseEntity.ok(gymService.getMainPageGyms(lat, lng)));
    }

    // --- Admin Endpoints ---

    @PostMapping("/admin/gyms")
    @Operation(summary = "Create gym (Admin)", description = "Creates a new gym profile. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Gym created successfully") })
    public Mono<ResponseEntity<Void>> createGym(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymService.createGym(body);
            return ResponseEntity.status(201).build();
        });
    }

    @PutMapping("/admin/gyms/{gymId}")
    @Operation(summary = "Update gym (Admin)", description = "Updates an existing gym profile. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> updateGym(@PathVariable Long gymId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymService.updateGym(gymId, body);
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping("/admin/gyms/{gymId}/packages")
    @Operation(summary = "Add package (Admin)", description = "Adds a subscription package to a gym. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> addPackage(@PathVariable Long gymId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymService.addPackage(gymId, body);
            return ResponseEntity.status(201).build();
        });
    }

    @PutMapping("/admin/gyms/{gymId}/packages/{packageId}")
    @Operation(summary = "Update package (Admin)", description = "Updates a gym package. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> updatePackage(@PathVariable Long gymId, @PathVariable Long packageId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymService.updatePackage(gymId, packageId, body);
            return ResponseEntity.noContent().build();
        });
    }

    @DeleteMapping("/admin/gyms/{gymId}/packages/{packageId}")
    @Operation(summary = "Delete package (Admin)", description = "Deletes a gym package. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> deletePackage(@PathVariable Long gymId, @PathVariable Long packageId) {
        return Mono.fromCallable(() -> {
            gymService.deletePackage(gymId, packageId);
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping("/admin/gyms/{gymId}/trainers")
    @Operation(summary = "Add trainer (Admin)", description = "Registers a trainer for a gym. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> addTrainer(@PathVariable Long gymId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymService.addTrainer(gymId, body);
            return ResponseEntity.status(201).build();
        });
    }

    @PutMapping("/admin/gyms/{gymId}/trainers/{trainerId}")
    @Operation(summary = "Update trainer (Admin)", description = "Updates trainer profiles. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> updateTrainer(@PathVariable Long gymId, @PathVariable Long trainerId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymService.updateTrainer(gymId, trainerId, body);
            return ResponseEntity.noContent().build();
        });
    }

    @DeleteMapping("/admin/gyms/{gymId}/trainers/{trainerId}")
    @Operation(summary = "Delete trainer (Admin)", description = "Deletes a trainer from a gym. Requires ADMIN role.")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> deleteTrainer(@PathVariable Long gymId, @PathVariable Long trainerId) {
        return Mono.fromCallable(() -> {
            gymService.deleteTrainer(gymId, trainerId);
            return ResponseEntity.noContent().build();
        });
    }
}
