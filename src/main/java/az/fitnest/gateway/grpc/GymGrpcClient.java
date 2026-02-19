package az.fitnest.gateway.grpc;

import marketplace.GetGymDetailRequest;
import marketplace.GymDetailResponse;
import marketplace.GymServiceGrpc;
import marketplace.GetGymImagesRequest;
import marketplace.GymImageResponse;
import marketplace.GetMainPageGymsRequest;
import marketplace.GetMainPageGymsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class GymGrpcClient {

    @GrpcClient("marketplace-service")
    private GymServiceGrpc.GymServiceBlockingStub gymStub;

    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        GetGymDetailRequest request = GetGymDetailRequest.newBuilder()
                .setUserId(userId != null ? userId : 0L)
                .setGymId(gymId)
                .build();
        return gymStub.getGymDetail(request);
    }

    public marketplace.GymImageResponse getGymImages(Long gymId) {
        return gymStub.getGymImages(marketplace.GetGymImagesRequest.newBuilder()
                .setGymId(gymId)
                .build());
    }

    public marketplace.GymPackagesResponse getGymPackages(Long gymId) {
        return gymStub.getGymPackages(marketplace.GetGymPackagesRequest.newBuilder()
                .setGymId(gymId)
                .build());
    }

    public marketplace.GymPackageIncludesResponse getPackageIncludes(Long gymId, Long packageId) {
        return gymStub.getPackageIncludes(marketplace.GetPackageIncludesRequest.newBuilder()
                .setGymId(gymId)
                .setPackageId(packageId)
                .build());
    }

    public marketplace.GymTrainersResponse getTrainers(Long gymId, int page, int pageSize) {
        return gymStub.getTrainers(marketplace.GetTrainersRequest.newBuilder()
                .setGymId(gymId)
                .setPage(page)
                .setPageSize(pageSize)
                .build());
    }

    public marketplace.GymReviewsResponse getReviews(Long gymId, int page, int pageSize, String sort) {
        return gymStub.getReviews(marketplace.GetReviewsRequest.newBuilder()
                .setGymId(gymId)
                .setPage(page)
                .setPageSize(pageSize)
                .setSort(sort != null ? sort : "")
                .build());
    }

    public boolean addReview(Long userId, Long gymId, Integer rating, String comment) {
        marketplace.AddReviewResponse response = gymStub.addReview(marketplace.AddReviewRequest.newBuilder()
                .setUserId(userId)
                .setGymId(gymId)
                .setRating(rating)
                .setComment(comment != null ? comment : "")
                .build());
        return response.getSuccess();
    }

    public marketplace.ReservationRulesResponse getReservationRules(Long gymId) {
        return gymStub.getReservationRules(marketplace.GetReservationRulesRequest.newBuilder()
                .setGymId(gymId)
                .build());
    }

    public marketplace.GetMainPageGymsResponse getMainPageGyms(Double lat, Double lng) {
        return gymStub.getMainPageGyms(marketplace.GetMainPageGymsRequest.newBuilder()
                .setLat(lat != null ? lat : 0.0)
                .setLng(lng != null ? lng : 0.0)
                .build());
    }

    // Admin methods
    public boolean addPackage(Long gymId, String name, Double price, String currency, String billingPeriod, boolean isActive, int sortOrder) {
        return gymStub.addPackage(marketplace.AddPackageRequest.newBuilder()
                .setGymId(gymId)
                .setName(name)
                .setPrice(price)
                .setCurrency(currency)
                .setBillingPeriod(billingPeriod)
                .setIsActive(isActive)
                .setSortOrder(sortOrder)
                .build()).getSuccess();
    }

    public boolean updatePackage(Long gymId, Long packageId, String name, Double price, String currency, String billingPeriod, boolean isActive, int sortOrder) {
        return gymStub.updatePackage(marketplace.UpdatePackageRequest.newBuilder()
                .setGymId(gymId)
                .setPackageId(packageId)
                .setName(name)
                .setPrice(price)
                .setCurrency(currency)
                .setBillingPeriod(billingPeriod)
                .setIsActive(isActive)
                .setSortOrder(sortOrder)
                .build()).getSuccess();
    }

    public boolean deletePackage(Long gymId, Long packageId) {
        return gymStub.deletePackage(marketplace.DeletePackageRequest.newBuilder()
                .setGymId(gymId)
                .setPackageId(packageId)
                .build()).getSuccess();
    }

    public boolean addTrainer(Long gymId, String fullName, String specialization, String imageUrl) {
        return gymStub.addTrainer(marketplace.AddTrainerRequest.newBuilder()
                .setGymId(gymId)
                .setFullName(fullName)
                .setSpecialization(specialization)
                .setImageUrl(imageUrl != null ? imageUrl : "")
                .build()).getSuccess();
    }

    public boolean updateTrainer(Long gymId, Long trainerId, String fullName, String specialization, String imageUrl) {
        return gymStub.updateTrainer(marketplace.UpdateTrainerRequest.newBuilder()
                .setGymId(gymId)
                .setTrainerId(trainerId)
                .setFullName(fullName)
                .setSpecialization(specialization)
                .setImageUrl(imageUrl != null ? imageUrl : "")
                .build()).getSuccess();
    }

    public boolean deleteTrainer(Long gymId, Long trainerId) {
        return gymStub.deleteTrainer(marketplace.DeleteTrainerRequest.newBuilder()
                .setGymId(gymId)
                .setTrainerId(trainerId)
                .build()).getSuccess();
    }

    public boolean createGym(String name, String description, String status, String coverImageUrl, 
                             String addressText, Double lat, Double lng, String phone, String email, 
                             java.util.List<String> categoryIds) {
        marketplace.Address address = marketplace.Address.newBuilder()
                .setAddressText(addressText)
                .setLatitude(lat)
                .setLongitude(lng)
                .build();
        return gymStub.createGym(marketplace.CreateGymRequest.newBuilder()
                .setName(name)
                .setDescription(description)
                .setStatus(status)
                .setCoverImageUrl(coverImageUrl)
                .setAddress(address)
                .setPhone(phone)
                .setEmail(email)
                .addAllCategoryIds(categoryIds)
                .build()).getSuccess();
    }

    public boolean updateGym(Long gymId, String name, String description, String status, String coverImageUrl, 
                             String addressText, Double lat, Double lng, String phone, String email, 
                             java.util.List<String> categoryIds) {
        marketplace.Address address = marketplace.Address.newBuilder()
                .setAddressText(addressText)
                .setLatitude(lat)
                .setLongitude(lng)
                .build();
        return gymStub.updateGym(marketplace.UpdateGymRequest.newBuilder()
                .setGymId(gymId)
                .setName(name)
                .setDescription(description)
                .setStatus(status)
                .setCoverImageUrl(coverImageUrl)
                .setAddress(address)
                .setPhone(phone)
                .setEmail(email)
                .addAllCategoryIds(categoryIds)
                .build()).getSuccess();
    }
}
