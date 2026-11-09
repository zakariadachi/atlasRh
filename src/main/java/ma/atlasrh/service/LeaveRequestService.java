package ma.atlasrh.service;

import ma.atlasrh.dto.LeaveDecisionDTO;
import ma.atlasrh.dto.LeaveRequestCreateDTO;
import ma.atlasrh.dto.LeaveRequestResponseDTO;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.entity.LeaveStatus;
import ma.atlasrh.entity.LeaveType;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.LeaveRequestMapper;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import ma.atlasrh.repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveRequestService {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRequestMapper leaveRequestMapper;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               LeaveBalanceService leaveBalanceService,
                               LeaveRequestMapper leaveRequestMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveRequestMapper = leaveRequestMapper;
    }

    public LeaveRequestResponseDTO create(Long employeeId, LeaveRequestCreateDTO dto) {
        log.info("Création d'une demande de congé pour l'employé id={}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeeId));

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Type de congé", dto.getLeaveTypeId()));

        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate();

        if (end.isBefore(start)) {
            throw new BusinessRuleException(
                    "La date de fin ne peut pas être antérieure à la date de début");
        }

        int totalBusinessDays = BusinessDayCalculator.calculateBusinessDays(start, end);
        if (totalBusinessDays == 0) {
            throw new BusinessRuleException(
                    "La demande ne contient aucun jour ouvré (week-end uniquement)");
        }

        if (leaveRequestRepository.hasOverlappingLeave(employeeId, start, end)) {
            throw new ConflictException(
                    "Cette demande chevauche une demande existante");
        }

        // Vérification du solde (gestion à cheval sur deux années)
        int startYear = start.getYear();
        int endYear = end.getYear();

        if (startYear == endYear) {
            int daysNeeded = totalBusinessDays;
            int remaining = leaveBalanceService.getRemainingBalance(employeeId, leaveType.getId(), startYear);
            if (daysNeeded > remaining) {
                throw new BusinessRuleException(
                        "Solde insuffisant pour " + startYear + " : " + remaining + " jour(s) disponible(s), " + daysNeeded + " demandé(s)");
            }
        } else {
            int daysInStartYear = BusinessDayCalculator.calculateBusinessDaysForYear(start, end, startYear);
            int daysInEndYear = BusinessDayCalculator.calculateBusinessDaysForYear(start, end, endYear);

            int remainingStartYear = leaveBalanceService.getRemainingBalance(employeeId, leaveType.getId(), startYear);
            if (daysInStartYear > remainingStartYear) {
                throw new BusinessRuleException(
                        "Solde insuffisant pour " + startYear + " : " + remainingStartYear + " jour(s) disponible(s), " + daysInStartYear + " demandé(s)");
            }

            int remainingEndYear = leaveBalanceService.getRemainingBalance(employeeId, leaveType.getId(), endYear);
            if (daysInEndYear > remainingEndYear) {
                throw new BusinessRuleException(
                        "Solde insuffisant pour " + endYear + " : " + remainingEndYear + " jour(s) disponible(s), " + daysInEndYear + " demandé(s)");
            }
        }

        LeaveRequest leaveRequest = new LeaveRequest(employee, leaveType, start, end, totalBusinessDays, dto.getReason());
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé créée avec l'id {}", leaveRequest.getId());
        return leaveRequestMapper.toResponseDTO(leaveRequest);
    }

    public LeaveRequestResponseDTO cancel(Long leaveRequestId, Long employeeId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", leaveRequestId));

        if (!lr.getEmployee().getId().equals(employeeId)) {
            throw new BusinessRuleException("Seul l'employé concerné peut annuler sa demande");
        }

        if (lr.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Seule une demande EN_ATTENTE peut être annulée");
        }

        lr.setStatus(LeaveStatus.ANNULEE);
        return leaveRequestMapper.toResponseDTO(leaveRequestRepository.save(lr));
    }

    public LeaveRequestResponseDTO approve(Long leaveRequestId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", leaveRequestId));

        if (lr.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Une décision n'est possible que sur une demande EN_ATTENTE");
        }

        // Revérification du solde à la validation
        Long employeeId = lr.getEmployee().getId();
        Long leaveTypeId = lr.getLeaveType().getId();
        int startYear = lr.getStartDate().getYear();
        int endYear = lr.getEndDate().getYear();

        if (startYear == endYear) {
            int remaining = leaveBalanceService.getRemainingBalance(employeeId, leaveTypeId, startYear);
            if (lr.getBusinessDays() > remaining) {
                lr.setStatus(LeaveStatus.REFUSEE);
                lr.setComment("Solde insuffisant au moment de la validation");
                leaveRequestRepository.save(lr);
                throw new BusinessRuleException("Solde insuffisant : demande automatiquement refusée");
            }
        } else {
            int daysInStartYear = BusinessDayCalculator.calculateBusinessDaysForYear(lr.getStartDate(), lr.getEndDate(), startYear);
            int daysInEndYear = BusinessDayCalculator.calculateBusinessDaysForYear(lr.getStartDate(), lr.getEndDate(), endYear);
            int remainingStart = leaveBalanceService.getRemainingBalance(employeeId, leaveTypeId, startYear);
            int remainingEnd = leaveBalanceService.getRemainingBalance(employeeId, leaveTypeId, endYear);
            if (daysInStartYear > remainingStart || daysInEndYear > remainingEnd) {
                lr.setStatus(LeaveStatus.REFUSEE);
                lr.setComment("Solde insuffisant au moment de la validation");
                leaveRequestRepository.save(lr);
                throw new BusinessRuleException("Solde insuffisant : demande automatiquement refusée");
            }
        }

        lr.setStatus(LeaveStatus.APPROUVEE);
        return leaveRequestMapper.toResponseDTO(leaveRequestRepository.save(lr));
    }

    public LeaveRequestResponseDTO reject(Long leaveRequestId, LeaveDecisionDTO decision) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", leaveRequestId));

        if (lr.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Une décision n'est possible que sur une demande EN_ATTENTE");
        }

        if (decision.getComment() == null || decision.getComment().isBlank()) {
            throw new BusinessRuleException("Un refus exige obligatoirement un commentaire");
        }

        lr.setStatus(LeaveStatus.REFUSEE);
        lr.setComment(decision.getComment());
        return leaveRequestMapper.toResponseDTO(leaveRequestRepository.save(lr));
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponseDTO findById(Long id) {
        return leaveRequestMapper.toResponseDTO(
                leaveRequestRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", id)));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDTO> getEmployeeLeaves(Long employeeId, LeaveStatus status) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé", employeeId);
        }
        List<LeaveRequest> results = (status != null)
                ? leaveRequestRepository.findByEmployeeIdAndStatusOrderByStartDateDesc(employeeId, status)
                : leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
        return results.stream().map(leaveRequestMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDTO> getPendingRequestsForManager(Long managerId) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager", managerId);
        }
        return leaveRequestRepository.findByEmployeeManagerIdAndStatus(managerId, LeaveStatus.EN_ATTENTE)
                .stream().map(leaveRequestMapper::toResponseDTO).collect(Collectors.toList());
    }
}
