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
