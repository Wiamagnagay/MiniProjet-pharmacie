package pharmacie.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@Service
public class ApprovisionnementService {

    private final MedicamentRepository medicamentRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public ApprovisionnementService(MedicamentRepository medicamentRepository, JavaMailSender mailSender) {
        this.medicamentRepository = medicamentRepository;
        this.mailSender = mailSender;
    }

    public void traiterReapprovisionnement() {
        List<Medicament> aReappro = medicamentRepository.findAll().stream()
                .filter(m -> m.getUnitesEnStock() < m.getNiveauDeReappro())
                .collect(Collectors.toList());

        if (aReappro.isEmpty()) {
            System.out.println("Aucun médicament à réapprovisionner.");
            return;
        }

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
        StringBuilder corps = new StringBuilder("Bonjour " + f.getNom() + ",\n\n");
        corps.append("Voici la liste des produits à réapprovisionner :\n");

        Map<String, List<Medicament>> parCat = medicaments.stream()
                .collect(Collectors.groupingBy(m -> m.getCategorie().getLibelle()));

        parCat.forEach((cat, meds) -> {
            corps.append("\nCatégorie : ").append(cat).append("\n");
            for (Medicament m : meds) {
                corps.append("- ").append(m.getNom())
                     .append(" (Réf: ").append(m.getReference())
                     .append(") Stock actuel: ").append(m.getUnitesEnStock()).append("\n");
            }
        });

        corps.append("\nMerci de nous transmettre un devis dans les plus brefs délais.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(f.getEmail());
        message.setSubject("Demande de devis réapprovisionnement - Pharmacie");
        message.setText(corps.toString());

        mailSender.send(message);
        System.out.println("Mail envoyé à " + f.getEmail());
    }
}