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

import java.time.LocalDateTime;
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

    /**
     * Dépôt d'une demande de congé par un employé (US5).
     */
    public LeaveRequestResponseDTO create(Long employeeId, LeaveRequestCreateDTO dto) {
        log.info("Dépôt d'une demande de congé pour l'employé id={}, typeId={}", employeeId, dto.getLeaveTypeId());

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé avec l'id " + employeeId + " introuvable"));

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Type de congé avec l'id " + dto.getLeaveTypeId() + " introuvable"));

        // Règle 1 : Date de fin >= Date de début
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessRuleException(
                    "La date de fin ne peut pas être antérieure à la date de début");
        }

        // Règle 2 : Calcul en jours ouvrés (lundi au vendredi, dates comprises)
        int businessDays = BusinessDayCalculator.calculateBusinessDays(dto.getStartDate(), dto.getEndDate());
        if (businessDays == 0) {
            throw new BusinessRuleException(
                    "La période sélectionnée ne comporte aucun jour ouvré (du lundi au vendredi)");
        }

        // Règle 3 : Interdiction des demandes qui se chevauchent pour un même employé
        if (leaveRequestRepository.hasOverlappingLeave(employeeId, dto.getStartDate(), dto.getEndDate())) {
            throw new ConflictException(
                    "L'employé a déjà une demande de congé qui chevauche la période demandée.");
        }

        // Règle 4 : Refus automatique d'une demande dépassant le solde pour chaque année concernée
        int startYear = dto.getStartDate().getYear();
        int endYear = dto.getEndDate().getYear();
        for (int y = startYear; y <= endYear; y++) {
            int daysForYear = BusinessDayCalculator.calculateBusinessDaysForYear(dto.getStartDate(), dto.getEndDate(), y);
            if (daysForYear > 0) {
                int remainingBalance = leaveBalanceService.getRemainingBalance(employeeId, leaveType.getId(), y);
                if (daysForYear > remainingBalance) {
                    String yearSuffix = (startYear != endYear) ? " (année " + y + ")" : "";
                    throw new BusinessRuleException(
                            "Solde insuffisant pour le type de congé '" + leaveType.getName() + "'" + yearSuffix + ". "
                                    + "Solde restant : " + remainingBalance + " jour(s), demandé : " + daysForYear + " jour(s)");
                }
            }
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setBusinessDays(businessDays);
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.EN_ATTENTE);
        leaveRequest.setCreatedAt(LocalDateTime.now());

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Demande de congé créée avec succès id={}", leaveRequest.getId());

        return leaveRequestMapper.toResponseDTO(leaveRequest);
    }

    /**
     * Annulation d'une demande par l'employé (US5).
     * Règle : Seul l'employé peut annuler sa demande, et uniquement si elle est EN_ATTENTE.
     */
    public LeaveRequestResponseDTO cancel(Long requestId, Long employeeId) {
        log.info("Annulation de la demande de congé id={} par employé id={}", requestId, employeeId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande de congé avec l'id " + requestId + " introuvable"));

        // Seul l'employé concerné peut annuler sa demande
        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new BusinessRuleException(
                    "Seul l'employé concerné est autorisé à annuler sa demande de congé");
        }

        // Seule une demande EN_ATTENTE peut être annulée
        if (leaveRequest.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Seule une demande au statut EN_ATTENTE peut être annulée (statut actuel : "
                            + leaveRequest.getStatus() + ")");
        }

        leaveRequest.setStatus(LeaveStatus.ANNULEE);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé id={} annulée", requestId);
        return leaveRequestMapper.toResponseDTO(leaveRequest);
    }

    /**
     * Validation d'une demande par le manager (US4).
     * Règle : Possible uniquement sur une demande EN_ATTENTE.
     */
    public LeaveRequestResponseDTO approve(Long requestId) {
        log.info("Approbation de la demande de congé id={}", requestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande de congé avec l'id " + requestId + " introuvable"));

        if (leaveRequest.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Une décision est possible uniquement sur une demande au statut EN_ATTENTE (statut actuel : "
                            + leaveRequest.getStatus() + ")");
        }

        // Vérification de sécurité sur le solde pour chaque année concernée
        int startYear = leaveRequest.getStartDate().getYear();
        int endYear = leaveRequest.getEndDate().getYear();
        for (int y = startYear; y <= endYear; y++) {
            int daysForYear = BusinessDayCalculator.calculateBusinessDaysForYear(
                    leaveRequest.getStartDate(), leaveRequest.getEndDate(), y);
            if (daysForYear > 0) {
                int remaining = leaveBalanceService.getRemainingBalance(
                        leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), y);
                if (daysForYear > remaining) {
                    String yearSuffix = (startYear != endYear) ? " pour l'année " + y : "";
                    throw new BusinessRuleException(
                            "Impossible d'approuver : le solde disponible" + yearSuffix + " (" + remaining + " jours) est inférieur "
                                    + "au nombre de jours demandés (" + daysForYear + " jours)");
                }
            }
        }

        leaveRequest.setStatus(LeaveStatus.APPROUVEE);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé id={} approuvée avec succès", requestId);
        return leaveRequestMapper.toResponseDTO(leaveRequest);
    }

    /**
     * Refus d'une demande par le manager (US4).
     * Règle : Possible uniquement sur une demande EN_ATTENTE, et commentaire obligatoire.
     */
    public LeaveRequestResponseDTO reject(Long requestId, LeaveDecisionDTO decisionDTO) {
        log.info("Refus de la demande de congé id={}", requestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande de congé avec l'id " + requestId + " introuvable"));

        if (leaveRequest.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new BusinessRuleException(
                    "Une décision est possible uniquement sur une demande au statut EN_ATTENTE (statut actuel : "
                            + leaveRequest.getStatus() + ")");
        }

        // Règle : Un refus exige un commentaire
        if (decisionDTO == null || decisionDTO.getComment() == null || decisionDTO.getComment().trim().isEmpty()) {
            throw new BusinessRuleException(
                    "Le refus d'une demande de congé exige obligatoirement un commentaire expliquant le motif.");
        }

        leaveRequest.setStatus(LeaveStatus.REFUSEE);
        leaveRequest.setComment(decisionDTO.getComment().trim());
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Demande de congé id={} refusée avec commentaire", requestId);
        return leaveRequestMapper.toResponseDTO(leaveRequest);
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponseDTO findById(Long requestId) {
        log.debug("Consultation de la demande de congé id={}", requestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande de congé avec l'id " + requestId + " introuvable"));

        return leaveRequestMapper.toResponseDTO(leaveRequest);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDTO> getEmployeeLeaves(Long employeeId) {
        log.debug("Listing des demandes de congé pour l'employé id={}", employeeId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(
                    "Employé avec l'id " + employeeId + " introuvable");
        }

        return leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId).stream()
                .map(leaveRequestMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDTO> getPendingRequestsForManager(Long managerId) {
        log.debug("Listing des demandes en attente pour le manager id={}", managerId);

        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException(
                    "Manager avec l'id " + managerId + " introuvable");
        }

        return leaveRequestRepository.findByEmployeeManagerIdAndStatus(managerId, LeaveStatus.EN_ATTENTE).stream()
                .map(leaveRequestMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
