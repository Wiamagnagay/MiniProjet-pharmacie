package pharmacie.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pharmacie.service.MailService;

@RestController
public class TestMailController {

    private final MailService mailService;

    public TestMailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/test-mail")
    public String testMail() {
        mailService.envoyerMail(
                "agnagayw@gmail.com",
                "Test SendGrid",
                "Ceci est un test depuis Spring Boot"
        );
        return "Mail envoyé";
    }
}
