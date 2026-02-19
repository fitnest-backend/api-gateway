package az.fitnest.gateway.security.support;

import java.util.Set;

public class PathValidator {

    private static final Set<String> CSRF_EXEMPTED_PATHS = Set.of();

    private static final Set<String> AUTH_REQUIRED_PATHS = Set.of(
            "/api/v1/stores",
            "/api/v1/stores/",
            "/api/v1/stores/admin",
            "/api/v1/stores/admin/"
    );

    public static boolean requiresAuthForContent(String path) {
        // Content-modifying endpoints that should always require auth can be added here.
        // For now, treat any admin or stores endpoints that modify state as requiring auth.
        return path.startsWith("/api/v1/stores") || path.startsWith("/api/v1/marketplace/");
    }

    private PathValidator() {
    }

    public static boolean isCsrfExempted(String path) {
        return CSRF_EXEMPTED_PATHS.contains(path);
    }

    public static boolean requiresAuth(String path) {
        if (path.startsWith("/api/v1/goals/images/")) {
            return false;
        }
        // Require auth for store-related endpoints (create/update/delete) and other protected areas
        if (path.startsWith("/api/v1/stores")) {
            return true;
        }
        return path.startsWith("/api/v1/me") ||
               path.startsWith("/api/v1/internal/") ||
               path.startsWith("/api/v1/media/upload") ||
               path.startsWith("/api/v1/media/delete") ||
               path.startsWith("/api/v1/media/move") ||
               path.startsWith("/api/v1/marketplace/") ||
               path.startsWith("/api/v1/gyms/") ||
               path.startsWith("/api/v1/checkout/") ||
               AUTH_REQUIRED_PATHS.contains(path) ||
               requiresAuthForContent(path);

    }



    public static boolean isAdminRoute(String path) {
        // Admin routes in this project are often under /api/v1/<resource>/admin or /api/v1/admin
        return path.startsWith("/api/v1/admin/") || path.contains("/admin");
    }

    public static boolean startsWithApiV1(String path) {
        return path.startsWith("/api/v1/");
    }
}
