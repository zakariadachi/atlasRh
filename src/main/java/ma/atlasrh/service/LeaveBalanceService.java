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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LeaveBalanceService {

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

    public int getRemainingBalance(Long employeeId, Long leaveTypeId, int year) {
        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Type de congé", leaveTypeId));

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<LeaveRequest> approved = leaveRequestRepository
                .findApprovedLeavesInYearRange(employeeId, leaveTypeId, yearStart, yearEnd);

        int usedDays = approved.stream()
                .mapToInt(lr -> BusinessDayCalculator.calculateBusinessDaysForYear(lr.getStartDate(), lr.getEndDate(), year))
                .sum();

        return leaveType.getDaysPerYear() - usedDays;
    }

    public EmployeeLeaveBalanceDTO getEmployeeBalances(Long employeeId, Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeeId));

        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        final int targetYear = year;
        List<LeaveBalanceItemDTO> balances = leaveTypes.stream().map(lt -> {
            LocalDate yearStart = LocalDate.of(targetYear, 1, 1);
            LocalDate yearEnd = LocalDate.of(targetYear, 12, 31);

            List<LeaveRequest> approved = leaveRequestRepository
                    .findApprovedLeavesInYearRange(employeeId, lt.getId(), yearStart, yearEnd);

            int usedDays = approved.stream()
                    .mapToInt(lr -> BusinessDayCalculator.calculateBusinessDaysForYear(lr.getStartDate(), lr.getEndDate(), targetYear))
                    .sum();

            return new LeaveBalanceItemDTO(lt.getId(), lt.getName(),
                    lt.getDaysPerYear(), usedDays, lt.getDaysPerYear() - usedDays);
        }).collect(Collectors.toList());

        Period seniority = Period.between(employee.getHireDate(), LocalDate.now());

        EmployeeLeaveBalanceDTO dto = new EmployeeLeaveBalanceDTO();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeFullName(employee.getFirstName() + " " + employee.getLastName());
        dto.setYear(targetYear);
        dto.setSeniority(seniority.getYears() + " an(s) et " + seniority.getMonths() + " mois");
        dto.setBalances(balances);
        return dto;
    }
}
