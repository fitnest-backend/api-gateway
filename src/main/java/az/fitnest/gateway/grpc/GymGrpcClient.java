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

    public GymImageResponse getGymImages(Long gymId) {
        GetGymImagesRequest request = GetGymImagesRequest.newBuilder()
                .setGymId(gymId)
                .build();
        return gymStub.getGymImages(request);
    }

    public GetMainPageGymsResponse getMainPageGyms(Double lat, Double lng) {
        GetMainPageGymsRequest request = GetMainPageGymsRequest.newBuilder()
                .setLat(lat != null ? lat : 0.0)
                .setLng(lng != null ? lng : 0.0)
                .build();
        return gymStub.getMainPageGyms(request);
    }
}
