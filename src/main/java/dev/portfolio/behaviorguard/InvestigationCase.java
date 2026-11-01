package dev.portfolio.behaviorguard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvestigationCase(
        UUID id,
        String userId,
        String title,
        String severity,
        String status,
        Instant createdAt,
        List<String> evidenceEventIds) {
    public InvestigationCase {
        evidenceEventIds = List.copyOf(evidenceEventIds);
    }
}