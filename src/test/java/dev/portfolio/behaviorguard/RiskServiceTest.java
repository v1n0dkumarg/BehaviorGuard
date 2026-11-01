package dev.portfolio.behaviorguard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.mock;

class RiskServiceTest {
    @Test
    void demoScenarioProducesExplainableScoreAndEvidence() {
        RiskService service = new RiskService(mock(JdbcTemplate.class));

        InvestigationCase investigation = service.simulateAccountTakeover();

        assertEquals("CRITICAL", investigation.severity());
        assertEquals(2, investigation.evidenceEventIds().size());
    }

    @Test
    void riskScoreIsCappedAtOneHundred() {
        List<RiskFactor> factors = List.of(
            new RiskFactor("A", "First", 80, "Evidence", List.of("evt-1")),
            new RiskFactor("B", "Second", 50, "Evidence", List.of("evt-2")));

        assertEquals(100, RiskService.scoreFor(factors));
    }
}