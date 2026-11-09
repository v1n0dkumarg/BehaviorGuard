package dev.portfolio.behaviorguard;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskService {
    private final JdbcTemplate jdbc;

    public RiskService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void seedDemoProfiles() {
        Integer profileCount = jdbc.queryForObject("SELECT COUNT(*) FROM user_risk_profiles", Integer.class);
        if (profileCount == null || profileCount == 0) {
            insertProfile(user("usr-104", "Maya Chen", "Engineering", 18, "LOW",
                    "Usual sign-in: 08:00–18:00 · United States · 2 known devices", List.of()));
            insertProfile(user("usr-208", "Jonah Brooks", "Finance", 31, "GUARDED",
                    "Usual sign-in: 07:00–19:00 · United States · 1 known device", List.of()));
            insertProfile(user("svc-031", "Inventory service", "Platform", 9, "LOW",
                    "Expected: non-interactive · internal network · scheduled jobs", List.of()));
        }
    }

    @Cacheable("behavior-risk")
    public List<UserRisk> leaderboard() {
        List<String> userIds = jdbc.query("SELECT user_id FROM user_risk_profiles ORDER BY score DESC, user_id",
                (rs, row) -> rs.getString("user_id"));
        return userIds.stream().map(this::loadProfile).toList();
    }

    @Cacheable("behavior-cases")
    public List<InvestigationCase> cases() {
        return jdbc.query("SELECT id, user_id, title, severity, status, created_at FROM investigation_cases "
                        + "ORDER BY created_at DESC",
                (rs, row) -> {
                    UUID id = UUID.fromString(rs.getString("id"));
                    List<String> evidence = jdbc.query("SELECT event_id FROM case_evidence WHERE case_id = ? ORDER BY position",
                            (evidenceRs, evidenceRow) -> evidenceRs.getString("event_id"), id);
                    return new InvestigationCase(id, rs.getString("user_id"), rs.getString("title"),
                            rs.getString("severity"), rs.getString("status"),
                            rs.getTimestamp("created_at").toInstant(), evidence);
                });
    }

    @Transactional
    @CacheEvict(cacheNames = {"behavior-risk", "behavior-cases"}, allEntries = true)
    public synchronized InvestigationCase simulateAccountTakeover() {
        String userId = "usr-104";
        List<RiskFactor> factors = List.of(
                factor("NEW_COUNTRY", "New country", 25, "Sign-in country is outside the user's observed baseline.", "evt-bg-001"),
                factor("IMPOSSIBLE_TRAVEL", "Impossible travel", 20, "The locations are not reachable within the observed time gap.", "evt-bg-001", "evt-bg-002"),
                factor("NEW_DEVICE", "New device", 15, "The device has not appeared in the user's recent baseline.", "evt-bg-002"),
                factor("UNUSUAL_HOUR", "Unusual hour", 12, "Activity occurred outside the usual sign-in window.", "evt-bg-002"),
                factor("THREAT_IP", "Threat-intelligence match", 20, "The synthetic source IP matches a demo indicator.", "evt-bg-002"));
        UserRisk updated = user(userId, "Maya Chen", "Engineering", scoreFor(factors), "CRITICAL",
                "Usual sign-in: 08:00–18:00 · United States · 2 known devices", factors);
        updateProfile(updated);
        InvestigationCase investigation = new InvestigationCase(UUID.randomUUID(), userId,
                "Account takeover indicators across sign-in and device activity", "CRITICAL", "OPEN", Instant.now(),
                factors.stream().flatMap(factor -> factor.evidenceEventIds().stream()).distinct().toList());
        jdbc.update("INSERT INTO investigation_cases (id, user_id, title, severity, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
            investigation.id(), investigation.userId(), investigation.title(), investigation.severity(),
            investigation.status(), Timestamp.from(investigation.createdAt()));
        for (int index = 0; index < investigation.evidenceEventIds().size(); index++) {
            jdbc.update("INSERT INTO case_evidence (case_id, event_id, position) VALUES (?, ?, ?)",
                investigation.id(), investigation.evidenceEventIds().get(index), index);
        }
        return investigation;
    }

    static int scoreFor(List<RiskFactor> factors) {
        return Math.min(100, factors.stream().mapToInt(RiskFactor::points).sum());
    }

    private static RiskFactor factor(String code, String label, int points, String explanation, String... eventIds) {
        return new RiskFactor(code, label, points, explanation, List.of(eventIds));
    }

    private static UserRisk user(String id, String name, String department, int score, String tier,
            String baseline, List<RiskFactor> factors) {
        return new UserRisk(id, name, department, score, tier, baseline, Instant.now(), factors);
    }

        private void insertProfile(UserRisk profile) {
        jdbc.update("INSERT INTO user_risk_profiles (user_id, display_name, department, score, tier, baseline, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            profile.userId(), profile.displayName(), profile.department(), profile.score(), profile.tier(),
            profile.baseline(), Timestamp.from(profile.updatedAt()));
        }

        private void updateProfile(UserRisk profile) {
        jdbc.update("INSERT INTO user_risk_profiles (user_id, display_name, department, score, tier, baseline, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id) DO UPDATE SET "
                + "display_name = EXCLUDED.display_name, department = EXCLUDED.department, score = EXCLUDED.score, "
                + "tier = EXCLUDED.tier, baseline = EXCLUDED.baseline, updated_at = EXCLUDED.updated_at",
            profile.userId(), profile.displayName(), profile.department(), profile.score(), profile.tier(),
            profile.baseline(), Timestamp.from(profile.updatedAt()));
        jdbc.update("DELETE FROM risk_factors WHERE user_id = ?", profile.userId());
        for (int index = 0; index < profile.factors().size(); index++) {
            RiskFactor factor = profile.factors().get(index);
            UUID factorId = UUID.randomUUID();
            jdbc.update("INSERT INTO risk_factors (id, user_id, position, code, label, points, explanation) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                factorId, profile.userId(), index, factor.code(), factor.label(), factor.points(), factor.explanation());
            for (int evidenceIndex = 0; evidenceIndex < factor.evidenceEventIds().size(); evidenceIndex++) {
            jdbc.update("INSERT INTO risk_factor_evidence (factor_id, event_id, position) VALUES (?, ?, ?)",
                factorId, factor.evidenceEventIds().get(evidenceIndex), evidenceIndex);
            }
        }
        }

        private UserRisk loadProfile(String userId) {
        UserRisk profile = jdbc.queryForObject("SELECT user_id, display_name, department, score, tier, baseline, updated_at "
                + "FROM user_risk_profiles WHERE user_id = ?",
            (rs, row) -> new UserRisk(rs.getString("user_id"), rs.getString("display_name"),
                rs.getString("department"), rs.getInt("score"), rs.getString("tier"),
                rs.getString("baseline"), rs.getTimestamp("updated_at").toInstant(), List.of()), userId);
        List<RiskFactor> factors = jdbc.query("SELECT id, code, label, points, explanation FROM risk_factors "
                + "WHERE user_id = ? ORDER BY position",
            (rs, row) -> {
                UUID factorId = UUID.fromString(rs.getString("id"));
                List<String> evidence = jdbc.query("SELECT event_id FROM risk_factor_evidence "
                        + "WHERE factor_id = ? ORDER BY position",
                    (evidenceRs, evidenceRow) -> evidenceRs.getString("event_id"), factorId);
                return new RiskFactor(rs.getString("code"), rs.getString("label"), rs.getInt("points"),
                    rs.getString("explanation"), evidence);
            }, userId);
        return new UserRisk(profile.userId(), profile.displayName(), profile.department(), profile.score(),
            profile.tier(), profile.baseline(), profile.updatedAt(), factors);
        }
}