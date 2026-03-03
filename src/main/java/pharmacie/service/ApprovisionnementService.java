package pharmacie.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@Service
public class ApprovisionnementService {

    private final MedicamentRepository medicamentRepository;

    @Value("${sendgrid.api-key:#{null}}")
    private String sendgridApiKey;

    @Value("${sendgrid.from-email:noreply@pharmacie.com}")
    private String fromEmail;

    public ApprovisionnementService(MedicamentRepository medicamentRepository) {
        this.medicamentRepository = medicamentRepository;
    }

    public void traiterReapprovisionnement() {
        List<Medicament> aReappro = medicamentRepository.findAll().stream()
                .filter(m -> m.getUnitesEnStock() < m.getNiveauDeReappro())
                .collect(Collectors.toList());

        if (aReappro.isEmpty()) return;

        Map<Fournisseur, List<Medicament>> envoisParFournisseur = new HashMap<>();
        for (Medicament m : aReappro) {
            for (Fournisseur f : m.getCategorie().getFournisseurs()) {
                envoisParFournisseur.computeIfAbsent(f, k -> new ArrayList<>()).add(m);
            }
        }

        for (Map.Entry<Fournisseur, List<Medicament>> entry : envoisParFournisseur.entrySet()) {
            envoyerMailAppro(entry.getKey(), entry.getValue());
        }
    }

    private void envoyerMailAppro(Fournisseur f, List<Medicament> medicaments) {
        // Si SendGrid n'est pas configuré , on fais rien
        if (sendgridApiKey == null) return;

        Email from = new Email(fromEmail);
        Email to = new Email(f.getEmail());
        String subject = "Demande de devis réapprovisionnement - Pharmacie";

        StringBuilder corps = new StringBuilder("Bonjour " + f.getNom() + ",\n\n");
        corps.append("Voici la liste des produits à réapprovisionner :\n");

        Map<String, List<Medicament>> parCat = medicaments.stream()
                .collect(Collectors.groupingBy(m -> m.getCategorie().getLibelle()));

        parCat.forEach((cat, meds) -> {
            corps.append("\nCatégorie : ").append(cat).append("\n");
            for (Medicament m : meds) {
                corps.append("- ").append(m.getNom())
                     .append(" (Réf: ").append(m.getReference())
                     .append(") - Stock actuel: ").append(m.getUnitesEnStock()).append("\n");
            }
        });

        corps.append("\nMerci de nous transmettre un devis dans les plus brefs délais.");

        Content content = new Content("text/plain", corps.toString());
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println("Mail envoyé à " + f.getEmail() + " - Status: " + response.getStatusCode());
        } catch (IOException e) {
            System.err.println("Erreur envoi mail à " + f.getEmail() + " : " + e.getMessage());
        }
    }
}