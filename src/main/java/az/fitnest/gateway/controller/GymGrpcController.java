package az.fitnest.gateway.controller;

import az.fitnest.gateway.grpc.GymGrpcClient;
import marketplace.GymDetailResponse;
import marketplace.GymImageResponse;
import marketplace.GetMainPageGymsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/gyms")
public class GymGrpcController {

    @Autowired
    private GymGrpcClient gymClient;

    @GetMapping("/{gymId}")
    public Mono<ResponseEntity<Object>> getGymDetail(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @PathVariable Long gymId) {
        
        return Mono.fromCallable(() -> {
            Long userId = (userIdStr != null && !userIdStr.isEmpty()) ? Long.parseLong(userIdStr) : null;
            GymDetailResponse response = gymClient.getGymDetail(userId, gymId);
            return ResponseEntity.ok((Object) responseToMap(response));
        });
    }

    @GetMapping("/{gymId}/images")
    public Mono<ResponseEntity<Object>> getGymImages(@PathVariable Long gymId) {
        return Mono.fromCallable(() -> {
            GymImageResponse response = gymClient.getGymImages(gymId);
            return ResponseEntity.ok((Object) response.getItemsList().stream()
                    .map(item -> Map.of(
                            "id", item.getImageId(),
                            "url", item.getUrl(),
                            "type", item.getType(),
                            "title", item.getTitle()
                    )).collect(Collectors.toList()));
        });
    }

    @GetMapping("/{gymId}/packages")
    public Mono<ResponseEntity<Object>> getGymPackages(@PathVariable Long gymId) {
        return Mono.fromCallable(() -> {
            marketplace.GymPackagesResponse response = gymClient.getGymPackages(gymId);
            return ResponseEntity.ok((Object) response.getItemsList().stream()
                    .map(item -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("plan_id", item.getPlanId());
                        map.put("name", item.getName());
                        map.put("price", item.getPrice());
                        map.put("currency", item.getCurrency());
                        map.put("billing_period", item.getBillingPeriod());
                        map.put("is_active", item.getIsActive());
                        map.put("sort_order", item.getSortOrder());
                        map.put("benefits", item.getBenefitsList().stream()
                                .map(b -> Map.of("logo", b.getLogo(), "description", b.getDescription()))
                                .collect(Collectors.toList()));
                        return map;
                    }).collect(Collectors.toList()));
        });
    }

    @GetMapping("/{gymId}/packages/{packageId}/includes")
    public Mono<ResponseEntity<Object>> getPackageIncludes(@PathVariable Long gymId, @PathVariable Long packageId) {
        return Mono.fromCallable(() -> {
            marketplace.GymPackageIncludesResponse response = gymClient.getPackageIncludes(gymId, packageId);
            return ResponseEntity.ok((Object) Map.of(
                    "plan_id", response.getPlanId(),
                    "items", response.getItemsList().stream()
                            .map(b -> Map.of("logo", b.getLogo(), "description", b.getDescription()))
                            .collect(Collectors.toList())
            ));
        });
    }

    @GetMapping("/{gymId}/trainers")
    public Mono<ResponseEntity<Object>> getTrainers(
            @PathVariable Long gymId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size) {
        return Mono.fromCallable(() -> {
            marketplace.GymTrainersResponse response = gymClient.getTrainers(gymId, page, page_size);
            return ResponseEntity.ok((Object) response.getItemsList().stream()
                    .map(item -> Map.of(
                            "trainer_id", item.getTrainerId(),
                            "full_name", item.getFullName(),
                            "specialization", item.getSpecialization(),
                            "image_url", item.getImageUrl()
                    )).collect(Collectors.toList()));
        });
    }

    @GetMapping("/{gymId}/reviews")
    public Mono<ResponseEntity<Object>> getReviews(
            @PathVariable Long gymId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int page_size,
            @RequestParam(required = false) String sort) {
        return Mono.fromCallable(() -> {
            marketplace.GymReviewsResponse response = gymClient.getReviews(gymId, page, page_size, sort);
            return ResponseEntity.ok((Object) response.getItemsList().stream()
                    .map(item -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("review_id", item.getReviewId());
                        map.put("rating", item.getRating());
                        map.put("comment", item.getComment());
                        map.put("created_at", item.getCreatedAt());
                        if (item.hasAuthor()) {
                            map.put("author", Map.of(
                                    "user_id", item.getAuthor().getUserId(),
                                    "full_name", item.getAuthor().getFullName(),
                                    "avatar_url", item.getAuthor().getAvatarUrl()
                            ));
                        }
                        return map;
                    }).collect(Collectors.toList()));
        });
    }

    @PostMapping("/{gymId}/reviews")
    public Mono<ResponseEntity<Void>> addReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long gymId,
            @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Integer rating = (Integer) body.get("rating");
            String comment = (String) body.get("comment");
            gymClient.addReview(userId, gymId, rating, comment);
            return ResponseEntity.status(201).<Void>build();
        });
    }

    @GetMapping("/{gymId}/reservation-rules")
    public Mono<ResponseEntity<Object>> getReservationRules(@PathVariable Long gymId) {
        return Mono.fromCallable(() -> {
            marketplace.ReservationRulesResponse response = gymClient.getReservationRules(gymId);
            return ResponseEntity.ok((Object) Map.of(
                    "reservation_required", response.getReservationRequired(),
                    "max_reservations_per_day", response.getMaxReservationsPerDay(),
                    "cancel_before_minutes", response.getCancelBeforeMinutes()
            ));
        });
    }

    @GetMapping("/main-page-gyms")
    public Mono<ResponseEntity<Object>> getMainPageGyms(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return Mono.fromCallable(() -> {
            GetMainPageGymsResponse response = gymClient.getMainPageGyms(lat, lng);
            return ResponseEntity.ok((Object) response.getItemsList().stream()
                    .map(item -> Map.of(
                            "gym_id", item.getGymId(),
                            "name", item.getName(),
                            "image_url", item.getImageUrl(),
                            "stars", item.getStars(),
                            "is_new", item.getIsNew(),
                            "location", item.getLocation(),
                            "distance_km", item.getDistanceKm()
                    )).collect(Collectors.toList()));
        });
    }

    // --- ADMIN ENDPOINTS ---

    @PostMapping("/admin/{gymId}/packages")
    public Mono<ResponseEntity<Void>> addPackage(@PathVariable Long gymId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymClient.addPackage(gymId, (String) body.get("name"), ((Number) body.get("price")).doubleValue(), 
                    (String) body.get("currency"), (String) body.get("billing_period"), 
                    (Boolean) body.getOrDefault("is_active", true), ((Number) body.getOrDefault("sort_order", 0)).intValue());
            return ResponseEntity.status(201).<Void>build();
        });
    }

    @PutMapping("/admin/{gymId}/packages/{packageId}")
    public Mono<ResponseEntity<Void>> updatePackage(@PathVariable Long gymId, @PathVariable Long packageId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymClient.updatePackage(gymId, packageId, (String) body.get("name"), ((Number) body.get("price")).doubleValue(), 
                    (String) body.get("currency"), (String) body.get("billing_period"), 
                    (Boolean) body.getOrDefault("is_active", true), ((Number) body.getOrDefault("sort_order", 0)).intValue());
            return ResponseEntity.noContent().<Void>build();
        });
    }

    @DeleteMapping("/admin/{gymId}/packages/{packageId}")
    public Mono<ResponseEntity<Void>> deletePackage(@PathVariable Long gymId, @PathVariable Long packageId) {
        return Mono.fromCallable(() -> {
            gymClient.deletePackage(gymId, packageId);
            return ResponseEntity.noContent().<Void>build();
        });
    }

    @PostMapping("/admin/{gymId}/trainers")
    public Mono<ResponseEntity<Void>> addTrainer(@PathVariable Long gymId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymClient.addTrainer(gymId, (String) body.get("full_name"), (String) body.get("specialization"), (String) body.get("image_url"));
            return ResponseEntity.status(201).<Void>build();
        });
    }

    @PutMapping("/admin/{gymId}/trainers/{trainerId}")
    public Mono<ResponseEntity<Void>> updateTrainer(@PathVariable Long gymId, @PathVariable Long trainerId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            gymClient.updateTrainer(gymId, trainerId, (String) body.get("full_name"), (String) body.get("specialization"), (String) body.get("image_url"));
            return ResponseEntity.noContent().<Void>build();
        });
    }

    @DeleteMapping("/admin/{gymId}/trainers/{trainerId}")
    public Mono<ResponseEntity<Void>> deleteTrainer(@PathVariable Long gymId, @PathVariable Long trainerId) {
        return Mono.fromCallable(() -> {
            gymClient.deleteTrainer(gymId, trainerId);
            return ResponseEntity.noContent().<Void>build();
        });
    }

    @PostMapping("/admin")
    public Mono<ResponseEntity<Void>> createGym(@RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Map<String, Object> address = (Map<String, Object>) body.get("address");
            java.util.List<String> categoryIds = (java.util.List<String>) body.get("category_ids");
            gymClient.createGym((String) body.get("name"), (String) body.get("description"), (String) body.get("status"), 
                    (String) body.get("cover_image_url"), (String) address.get("addressText"), 
                    ((Number) address.get("latitude")).doubleValue(), ((Number) address.get("longitude")).doubleValue(), 
                    (String) body.get("phone"), (String) body.get("email"), categoryIds);
            return ResponseEntity.status(201).<Void>build();
        });
    }

    @PutMapping("/admin/{gymId}")
    public Mono<ResponseEntity<Void>> updateGym(@PathVariable Long gymId, @RequestBody Map<String, Object> body) {
        return Mono.fromCallable(() -> {
            Map<String, Object> address = (Map<String, Object>) body.get("address");
            java.util.List<String> categoryIds = (java.util.List<String>) body.get("category_ids");
            gymClient.updateGym(gymId, (String) body.get("name"), (String) body.get("description"), (String) body.get("status"), 
                    (String) body.get("cover_image_url"), (String) address.get("addressText"), 
                    ((Number) address.get("latitude")).doubleValue(), ((Number) address.get("longitude")).doubleValue(), 
                    (String) body.get("phone"), (String) body.get("email"), categoryIds);
            return ResponseEntity.noContent().<Void>build();
        });
    }

    private Map<String, Object> responseToMap(GymDetailResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("gym_id", response.getGymId());
        map.put("name", response.getName());
        map.put("description", response.getDescription());
        map.put("status", response.getStatus());
        map.put("cover_image_url", response.getCoverImageUrl());
        map.put("address", Map.of(
                "addressText", response.getAddress().getAddressText(),
                "latitude", response.getAddress().getLatitude(),
                "longitude", response.getAddress().getLongitude()
        ));
        map.put("phone", response.getPhone());
        map.put("email", response.getEmail());
        map.put("rating", response.getRating());
        map.put("reviews_count", response.getReviewsCount());
        map.put("is_saved", response.getIsSaved());
        return map;
    }
}
