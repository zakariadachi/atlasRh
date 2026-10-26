package ma.atlasrh.repository;

import ma.atlasrh.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * Récupère l'historique des contrats d'un employé, triés par date de début décroissante.
     */
    List<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    /**
     * Vérifie si un employé a un contrat en cours (actif) à une date donnée.
     * Un contrat est en cours si :
     * - sa date de début est <= à la date donnée
     * - sa date de fin est null OU >= à la date donnée
     */
    @Query("SELECT COUNT(c) > 0 FROM Contract c WHERE c.employee.id = :employeeId " +
           "AND c.startDate <= :date " +
           "AND (c.endDate IS NULL OR c.endDate >= :date)")
    boolean hasActiveContract(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    /**
     * Vérifie s'il y a un chevauchement avec un contrat existant pour un employé.
     * Un nouveau contrat [newStart, newEnd] chevauche un contrat existant [startDate, endDate] si :
     * - newStart <= endDate (ou endDate est null) ET newEnd >= startDate (ou newEnd est null)
     */
    @Query("SELECT COUNT(c) > 0 FROM Contract c WHERE c.employee.id = :employeeId " +
           "AND c.startDate <= COALESCE(:newEnd, CAST('9999-12-31' AS localdate)) " +
           "AND (c.endDate IS NULL OR c.endDate >= :newStart)")
    boolean hasOverlappingContract(@Param("employeeId") Long employeeId,
                                    @Param("newStart") LocalDate newStart,
                                    @Param("newEnd") LocalDate newEnd);
}
