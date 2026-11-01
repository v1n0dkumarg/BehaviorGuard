package dev.portfolio.behaviorguard;

import java.util.List;

public record RiskFactor(String code, String label, int points, String explanation, List<String> evidenceEventIds) {
    public RiskFactor {
        evidenceEventIds = List.copyOf(evidenceEventIds);
    }
}