package ma.atlasrh.repository;

import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    boolean existsByEmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatusOrderByStartDateDesc(Long employeeId, LeaveStatus status);

    List<LeaveRequest> findByEmployeeManagerIdAndStatus(Long managerId, LeaveStatus status);

    @Query("SELECT COUNT(lr) > 0 FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.status NOT IN (ma.atlasrh.entity.LeaveStatus.REFUSEE, ma.atlasrh.entity.LeaveStatus.ANNULEE) " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    boolean hasOverlappingLeave(@Param("employeeId") Long employeeId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND lr.leaveType.id = :leaveTypeId " +
           "AND lr.status = ma.atlasrh.entity.LeaveStatus.APPROUVEE " +
           "AND lr.startDate <= :yearEnd AND lr.endDate >= :yearStart")
    List<LeaveRequest> findApprovedLeavesInYearRange(@Param("employeeId") Long employeeId,
                                                      @Param("leaveTypeId") Long leaveTypeId,
                                                      @Param("yearStart") LocalDate yearStart,
                                                      @Param("yearEnd") LocalDate yearEnd);
}
