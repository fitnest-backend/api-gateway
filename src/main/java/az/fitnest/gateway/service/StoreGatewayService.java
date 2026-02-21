package az.fitnest.gateway.service;

import az.fitnest.gateway.grpc.StoreGrpcClient;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreGatewayService {

    private final StoreGrpcClient storeGrpcClient;

    public String getMarketStores(String q, int page, int pageSize, Long userId) {
        return storeGrpcClient.getMarketStores(q, page, pageSize, userId);
    }

    public void toggleFavorite(String storeId, Long userId) {
        storeGrpcClient.toggleFavorite(storeId, userId);
    }

    public String createStoreAdmin(String body) {
        return storeGrpcClient.createStoreAdmin(body);
    }

    public String updateStoreAdmin(String storeId, String body) {
        return storeGrpcClient.updateStoreAdmin(storeId, body);
    }

    public void deleteStoreAdmin(String storeId) {
        storeGrpcClient.deleteStoreAdmin(storeId);
    }
}
