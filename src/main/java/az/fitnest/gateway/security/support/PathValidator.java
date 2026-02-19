package az.fitnest.gateway.security.support;

import java.util.Set;

public class PathValidator {

    private static final Set<String> CSRF_EXEMPTED_PATHS = Set.of();

    private static final Set<String> AUTH_REQUIRED_PATHS = Set.of(
            // Keep explicit admin-only paths here; don't include general stores routes
            "/api/v1/stores/admin",
            "/api/v1/stores/admin/"
    );

    public static boolean requiresAuthForContent(String path) {
        // Only treat clearly admin or internal content-modifying endpoints as requiring auth.
        // Previously we forced auth for all stores and marketplace content — that's stricter than identity.
        return path.startsWith("/api/v1/admin/");
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
        // Only require auth for admin, user-specific and internal or media protected endpoints.
        if (path.startsWith("/api/v1/admin/") || AUTH_REQUIRED_PATHS.contains(path)) {
            return true;
        }
        return path.startsWith("/api/v1/me") ||
               path.startsWith("/api/v1/internal/") ||
               path.startsWith("/api/v1/media/upload") ||
               path.startsWith("/api/v1/media/delete") ||
               path.startsWith("/api/v1/media/move");

    }


    public static boolean isAdminRoute(String path) {
        // Admin routes in this project are often under /api/v1/<resource>/admin or /api/v1/admin
        return path.startsWith("/api/v1/admin/") || path.contains("/admin");
    }

    public static boolean startsWithApiV1(String path) {
        return path.startsWith("/api/v1/");
    }
}
