package az.fitnest.gateway.shared.exception;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record ApiError(
    @JsonProperty("error")
    ErrorDetail error
) {
    @Builder
    public record ErrorDetail(
        String code,
        String message,
        int status,
        String path,
        OffsetDateTime timestamp,
        List<FieldIssue> details
    ) {}

    @Builder
    public record FieldIssue(
        String field,
        String issue
    ) {}
}
