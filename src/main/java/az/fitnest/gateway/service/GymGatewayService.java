package az.fitnest.gateway.service;

import az.fitnest.gateway.grpc.GymGrpcClient;
import marketplace.GymDetailResponse;
import marketplace.GymImageResponse;
import marketplace.GetMainPageGymsResponse;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymGatewayService {

    private final GymGrpcClient gymClient;

    public Map<String, Object> getGymDetail(Long userId, Long gymId) {
        GymDetailResponse response = gymClient.getGymDetail(userId, gymId);
        return responseToMap(response);
    }

    public List<Map<String, Object>> getGymImages(Long gymId) {
        GymImageResponse response = gymClient.getGymImages(gymId);
        return response.getItemsList().stream()
                .map(item -> Map.of(
                        "id", item.getImageId(),
                        "url", item.getUrl(),
                        "type", item.getType(),
                        "title", item.getTitle()
                )).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getGymPackages(Long gymId) {
        marketplace.GymPackagesResponse response = gymClient.getGymPackages(gymId);
        return response.getItemsList().stream()
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
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getPackageIncludes(Long gymId, Long packageId) {
        marketplace.GymPackageIncludesResponse response = gymClient.getPackageIncludes(gymId, packageId);
        return Map.of(
                "plan_id", response.getPlanId(),
                "items", response.getItemsList().stream()
                        .map(b -> Map.of("logo", b.getLogo(), "description", b.getDescription()))
                        .collect(Collectors.toList())
        );
    }

    public List<Map<String, Object>> getTrainers(Long gymId, int page, int pageSize) {
        marketplace.GymTrainersResponse response = gymClient.getTrainers(gymId, page, pageSize);
        return response.getItemsList().stream()
                .map(item -> Map.of(
                        "trainer_id", item.getTrainerId(),
                        "full_name", item.getFullName(),
                        "specialization", item.getSpecialization(),
                        "image_url", item.getImageUrl()
                )).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getReviews(Long gymId, int page, int pageSize, String sort) {
        marketplace.GymReviewsResponse response = gymClient.getReviews(gymId, page, pageSize, sort);
        return response.getItemsList().stream()
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
                }).collect(Collectors.toList());
    }

    public void addReview(Long userId, Long gymId, Integer rating, String comment) {
        gymClient.addReview(userId, gymId, rating, comment);
    }

    public Map<String, Object> getReservationRules(Long gymId) {
        marketplace.ReservationRulesResponse response = gymClient.getReservationRules(gymId);
        return Map.of(
                "reservation_required", response.getReservationRequired(),
                "max_reservations_per_day", response.getMaxReservationsPerDay(),
                "cancel_before_minutes", response.getCancelBeforeMinutes()
        );
    }

    public List<Map<String, Object>> getMainPageGyms(Double lat, Double lng) {
        GetMainPageGymsResponse response = gymClient.getMainPageGyms(lat, lng);
        return response.getItemsList().stream()
                .map(item -> Map.of(
                        "gym_id", item.getGymId(),
                        "name", item.getName(),
                        "image_url", item.getImageUrl(),
                        "stars", item.getStars(),
                        "is_new", item.getIsNew(),
                        "location", item.getLocation(),
                        "distance_km", item.getDistanceKm()
                )).collect(Collectors.toList());
    }

    public void createGym(Map<String, Object> body) {
        Map<String, Object> address = (Map<String, Object>) body.get("address");
        List<String> categoryIds = (List<String>) body.get("category_ids");
        gymClient.createGym((String) body.get("name"), (String) body.get("description"), (String) body.get("status"), 
                (String) body.get("cover_image_url"), (String) address.get("addressText"), 
                ((Number) address.get("latitude")).doubleValue(), ((Number) address.get("longitude")).doubleValue(), 
                (String) body.get("phone"), (String) body.get("email"), categoryIds);
    }

    public void updateGym(Long gymId, Map<String, Object> body) {
        Map<String, Object> address = (Map<String, Object>) body.get("address");
        List<String> categoryIds = (List<String>) body.get("category_ids");
        gymClient.updateGym(gymId, (String) body.get("name"), (String) body.get("description"), (String) body.get("status"), 
                (String) body.get("cover_image_url"), (String) address.get("addressText"), 
                ((Number) address.get("latitude")).doubleValue(), ((Number) address.get("longitude")).doubleValue(), 
                (String) body.get("phone"), (String) body.get("email"), categoryIds);
    }

    public void addPackage(Long gymId, Map<String, Object> body) {
        gymClient.addPackage(gymId, (String) body.get("name"), ((Number) body.get("price")).doubleValue(), 
                (String) body.get("currency"), (String) body.get("billing_period"), 
                (Boolean) body.getOrDefault("is_active", true), ((Number) body.getOrDefault("sort_order", 0)).intValue());
    }

    public void updatePackage(Long gymId, Long packageId, Map<String, Object> body) {
        gymClient.updatePackage(gymId, packageId, (String) body.get("name"), ((Number) body.get("price")).doubleValue(), 
                (String) body.get("currency"), (String) body.get("billing_period"), 
                (Boolean) body.getOrDefault("is_active", true), ((Number) body.getOrDefault("sort_order", 0)).intValue());
    }

    public void deletePackage(Long gymId, Long packageId) {
        gymClient.deletePackage(gymId, packageId);
    }

    public void addTrainer(Long gymId, Map<String, Object> body) {
        gymClient.addTrainer(gymId, (String) body.get("full_name"), (String) body.get("specialization"), (String) body.get("image_url"));
    }

    public void updateTrainer(Long gymId, Long trainerId, Map<String, Object> body) {
        gymClient.updateTrainer(gymId, trainerId, (String) body.get("full_name"), (String) body.get("specialization"), (String) body.get("image_url"));
    }

    public void deleteTrainer(Long gymId, Long trainerId) {
        gymClient.deleteTrainer(gymId, trainerId);
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
