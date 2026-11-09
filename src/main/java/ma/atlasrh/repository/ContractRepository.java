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

    // Historique des contrats d'un employé (plus récent au plus ancien)
    List<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);

    // Vérifie si un contrat est actif à une date donnée
    @Query("SELECT COUNT(c) > 0 FROM Contract c WHERE c.employee.id = :employeeId " +
           "AND c.startDate <= :date " +
           "AND (c.endDate IS NULL OR c.endDate >= :date)")
    boolean hasActiveContract(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    // Vérifie les chevauchements de dates pour un employé
    @Query("SELECT COUNT(c) > 0 FROM Contract c WHERE c.employee.id = :employeeId " +
           "AND c.startDate <= COALESCE(:newEnd, CAST('9999-12-31' AS localdate)) " +
           "AND (c.endDate IS NULL OR c.endDate >= :newStart)")
    boolean hasOverlappingContract(@Param("employeeId") Long employeeId,
                                    @Param("newStart") LocalDate newStart,
                                    @Param("newEnd") LocalDate newEnd);

    // Vérifie les chevauchements en ignorant un contrat précis (pour mise à jour)
    @Query("SELECT COUNT(c) > 0 FROM Contract c WHERE c.employee.id = :employeeId " +
           "AND c.id != :contractId " +
           "AND c.startDate <= COALESCE(:newEnd, CAST('9999-12-31' AS localdate)) " +
           "AND (c.endDate IS NULL OR c.endDate >= :newStart)")
    boolean hasOverlappingContractExcluding(@Param("employeeId") Long employeeId,
                                           @Param("contractId") Long contractId,
                                           @Param("newStart") LocalDate newStart,
                                           @Param("newEnd") LocalDate newEnd);
}
