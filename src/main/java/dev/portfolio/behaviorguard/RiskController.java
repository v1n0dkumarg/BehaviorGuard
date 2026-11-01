package dev.portfolio.behaviorguard;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RiskController {
    private final RiskService service;

    public RiskController(RiskService service) {
        this.service = service;
    }

    @GetMapping("/users/risk")
    public List<UserRisk> leaderboard() {
        return service.leaderboard();
    }

    @GetMapping("/cases")
    public List<InvestigationCase> cases() {
        return service.cases();
    }

    @PostMapping("/demo/account-takeover")
    @ResponseStatus(HttpStatus.CREATED)
    public InvestigationCase simulateAccountTakeover() {
        return service.simulateAccountTakeover();
    }
}