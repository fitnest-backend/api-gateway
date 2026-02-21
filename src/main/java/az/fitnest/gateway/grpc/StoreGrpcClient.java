package az.fitnest.gateway.grpc;

import az.fitnest.marketplace.grpc.StoreServiceGrpc;
import az.fitnest.marketplace.grpc.GetStoresRequest;
import az.fitnest.marketplace.grpc.StoreListResponse;
import az.fitnest.marketplace.grpc.ToggleFavoriteRequest;
import az.fitnest.marketplace.grpc.ToggleFavoriteResponse;
import az.fitnest.marketplace.grpc.CreateStoreAdminRequest;
import az.fitnest.marketplace.grpc.UpdateStoreAdminRequest;
import az.fitnest.marketplace.grpc.DeleteStoreAdminRequest;
import az.fitnest.marketplace.grpc.StoreDetailResponse;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class StoreGrpcClient {

    @GrpcClient("marketplace-service")
    private StoreServiceGrpc.StoreServiceBlockingStub storeStub;

    public String getMarketStores(String query, int page, int pageSize, Long userId) {
        GetStoresRequest request = GetStoresRequest.newBuilder()
                .setQuery(query != null ? query : "")
                .setPage(page)
                .setPageSize(pageSize)
                .setUserId(userId != null ? userId : 0L)
                .build();
        return storeStub.getMarketStores(request).getJsonPayload();
    }

    public boolean toggleFavorite(String storeId, Long userId) {
        ToggleFavoriteRequest request = ToggleFavoriteRequest.newBuilder()
                .setStoreId(storeId)
                .setUserId(userId != null ? userId : 0L)
                .build();
        return storeStub.toggleFavorite(request).getSuccess();
    }

    public String createStoreAdmin(String jsonPayload) {
        CreateStoreAdminRequest request = CreateStoreAdminRequest.newBuilder()
                .setJsonPayload(jsonPayload)
                .build();
        return storeStub.createStoreAdmin(request).getJsonPayload();
    }

    public String updateStoreAdmin(String storeId, String jsonPayload) {
        UpdateStoreAdminRequest request = UpdateStoreAdminRequest.newBuilder()
                .setStoreId(storeId)
                .setJsonPayload(jsonPayload)
                .build();
        return storeStub.updateStoreAdmin(request).getJsonPayload();
    }

    public void deleteStoreAdmin(String storeId) {
        DeleteStoreAdminRequest request = DeleteStoreAdminRequest.newBuilder()
                .setStoreId(storeId)
                .build();
        storeStub.deleteStoreAdmin(request);
    }
}
