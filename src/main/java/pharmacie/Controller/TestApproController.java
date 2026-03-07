package pharmacie.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pharmacie.service.ApprovisionnementService;

@RestController
public class TestApproController {

    private final ApprovisionnementService approService;

    public TestApproController(ApprovisionnementService approService) {
        this.approService = approService;
    }

    @GetMapping("/test-appro")
    public String tester() {
        approService.traiterReapprovisionnement();
        return "Terminé";
    }
}
//.