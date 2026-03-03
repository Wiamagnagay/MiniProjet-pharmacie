package pharmacie.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import pharmacie.entity.Fournisseur;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Integer> {

    
    @Query("""
        SELECT DISTINCT f FROM Fournisseur f
        JOIN f.categories c
        JOIN c.medicaments m
        WHERE m.unitesEnStock < m.niveauDeReappro
        AND m.niveauDeReappro > 0
    """)
    List<Fournisseur> fournisseursAvecMedicamentsAReapprovisionner();
}
