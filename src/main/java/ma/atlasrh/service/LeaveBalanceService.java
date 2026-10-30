package ma.atlasrh.service;

import ma.atlasrh.dto.EmployeeLeaveBalanceDTO;
import ma.atlasrh.dto.LeaveBalanceItemDTO;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.entity.LeaveType;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import ma.atlasrh.repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(LeaveBalanceService.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveBalanceService(EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Calcule le total des jours ouvrés des demandes APPROUVEE d'un employé pour un type donné,
     * répartis précisément sur l'année demandée.
     * Si une demande chevauche deux années, seuls les jours ouvrés appartenant à l'année demandée sont comptabilisés.
     */
    public int countApprovedDaysByYear(Long employeeId, Long leaveTypeId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesInYearRange(
                employeeId, leaveTypeId, yearStart, yearEnd);

        int totalDays = 0;
        for (LeaveRequest lr : approvedLeaves) {
            totalDays += BusinessDayCalculator.calculateBusinessDaysForYear(
                    lr.getStartDate(), lr.getEndDate(), year);
        }
        return totalDays;
    }

    /**
     * Calcule le solde restant pour un employé, un type et une année donnés.
     * Règle officielle : Solde = jours annuels du type - jours des demandes approuvées de l'année.
     */
    public int getRemainingBalance(Long employeeId, Long leaveTypeId, int year) {
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Type de congé avec l'id " + leaveTypeId + " introuvable"));

        int approvedDays = countApprovedDaysByYear(employeeId, leaveTypeId, year);
        int remaining = leaveType.getDaysPerYear() - approvedDays;

        log.debug("Solde calculé pour employé={}, type={}, année={} : {} jours restants ({} annuels - {} approuvés)",
                employeeId, leaveType.getName(), year, remaining, leaveType.getDaysPerYear(), approvedDays);

        return remaining;
    }

    /**
     * Récupère le bilan complet des soldes de congés de l'employé pour une année (par défaut l'année courante).
     */
    public EmployeeLeaveBalanceDTO getEmployeeBalances(Long employeeId, Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        log.info("Consultation des soldes de congés de l'employé id={} pour l'année {}", employeeId, targetYear);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé avec l'id " + employeeId + " introuvable"));

        List<LeaveType> types = leaveTypeRepository.findAll();
        List<LeaveBalanceItemDTO> items = new ArrayList<>();

        for (LeaveType type : types) {
            int approved = countApprovedDaysByYear(employeeId, type.getId(), targetYear);
            int remaining = type.getDaysPerYear() - approved;

            items.add(new LeaveBalanceItemDTO(
                    type.getId(),
                    type.getName(),
                    type.getDaysPerYear(),
                    approved,
                    remaining
            ));
        }

        String fullName = employee.getFirstName() + " " + employee.getLastName();
        return new EmployeeLeaveBalanceDTO(employeeId, fullName, targetYear, items);
    }
}
