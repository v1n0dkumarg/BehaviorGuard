package dev.portfolio.behaviorguard;

import java.time.Instant;
import java.util.List;

public record UserRisk(
        String userId,
        String displayName,
        String department,
        int score,
        String tier,
        String baseline,
        Instant updatedAt,
        List<RiskFactor> factors) {
    public UserRisk {
        factors = List.copyOf(factors);
    }
}