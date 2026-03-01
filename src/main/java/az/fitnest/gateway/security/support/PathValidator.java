package az.fitnest.gateway.security.support;

import java.util.Set;

public class PathValidator {

    private static final Set<String> CSRF_EXEMPTED_PATHS = Set.of();

    private static final Set<String> AUTH_REQUIRED_PATHS = Set.of();

    private PathValidator() {
    }

    public static boolean requiresAuthForContent(String path) {
        // Only treat clearly admin or internal content-modifying endpoints as requiring auth.
        // Previously we forced auth for all stores and catalog content — that's stricter than identity.
        return path.contains("/admin/") || path.endsWith("/admin");
    }

    public static boolean isCsrfExempted(String path) {
        return CSRF_EXEMPTED_PATHS.contains(path);
    }

    public static boolean requiresAuth(String path) {
        if (path.startsWith("/api/v1/goals/images/")) {
            return false;
        }

        // Global rule: Any path with /admin/ or /me/ (or just /me) requires authentication
        if (path.contains("/admin/") || path.endsWith("/admin") ||
                path.startsWith("/api/v1/me") || path.startsWith("/api/v1/internal/")) {
            return true;
        }

        // Specific protected endpoints
        return AUTH_REQUIRED_PATHS.contains(path) ||
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
