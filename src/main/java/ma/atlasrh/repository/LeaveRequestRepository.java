package ma.atlasrh.repository;

import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.entity.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    List<LeaveRequest> findByEmployeeManagerIdAndStatus(Long managerId, LeaveStatus status);

    /**
     * Vérifie s'il existe une demande active (EN_ATTENTE ou APPROUVEE) qui chevauche la période demandée.
     */
    @Query("SELECT COUNT(lr) > 0 FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.status IN (ma.atlasrh.entity.LeaveStatus.EN_ATTENTE, ma.atlasrh.entity.LeaveStatus.APPROUVEE) " +
           "AND lr.startDate <= :newEnd AND lr.endDate >= :newStart")
    boolean hasOverlappingLeave(@Param("employeeId") Long employeeId,
                                @Param("newStart") LocalDate newStart,
                                @Param("newEnd") LocalDate newEnd);

    /**
     * Calcule le total des jours ouvrés des demandes APPROUVEE pour un employé, un type et une année donnés.
     */
    @Query("SELECT COALESCE(SUM(lr.businessDays), 0) FROM LeaveRequest lr " +
           "WHERE lr.employee.id = :employeeId " +
           "AND lr.leaveType.id = :leaveTypeId " +
           "AND lr.status = ma.atlasrh.entity.LeaveStatus.APPROUVEE " +
           "AND YEAR(lr.startDate) = :year")
    int countApprovedDaysByYear(@Param("employeeId") Long employeeId,
                                @Param("leaveTypeId") Long leaveTypeId,
                                @Param("year") int year);
}
