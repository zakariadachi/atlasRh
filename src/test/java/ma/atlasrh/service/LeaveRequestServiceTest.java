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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee employee;
    private LeaveType leaveType;
    private LeaveRequest leaveRequest;
    private LeaveRequestCreateDTO createDTO;
    private LeaveRequestResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        employee = new Employee("ATL-005", "Mehdi", "Alaoui", "mehdi@atlasrh.ma", "Développeur", LocalDate.of(2022, 1, 1));
        employee.setId(5L);

        leaveType = new LeaveType("Annuel", "Congé annuel payé", 22);
        leaveType.setId(1L);

        // Lundi 2026-11-02 au Vendredi 2026-11-06 (5 jours ouvrés)
        LocalDate start = LocalDate.of(2026, 11, 2);
        LocalDate end = LocalDate.of(2026, 11, 6);

        createDTO = new LeaveRequestCreateDTO(1L, start, end, "Vacances d'automne");

        leaveRequest = new LeaveRequest(employee, leaveType, start, end, 5, "Vacances d'automne");
        leaveRequest.setId(10L);

        responseDTO = new LeaveRequestResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setEmployeeId(5L);
        responseDTO.setBusinessDays(5);
        responseDTO.setStatus(LeaveStatus.EN_ATTENTE);
    }

    @Test
    @DisplayName("Déposer une demande de congé avec succès au statut EN_ATTENTE")
    void create_shouldSucceed() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.hasOverlappingLeave(eq(5L), any(), any())).thenReturn(false);
        when(leaveBalanceService.getRemainingBalance(5L, 1L, 2026)).thenReturn(15);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);
        when(leaveRequestMapper.toResponseDTO(any(LeaveRequest.class))).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.create(5L, createDTO);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.EN_ATTENTE);
        assertThat(result.getBusinessDays()).isEqualTo(5);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Date de fin avant date de début doit lever BusinessRuleException")
    void create_endDateBeforeStartDate_shouldThrow() {
        createDTO.setStartDate(LocalDate.of(2026, 11, 10));
        createDTO.setEndDate(LocalDate.of(2026, 11, 5));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        assertThatThrownBy(() -> leaveRequestService.create(5L, createDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("antérieure");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Demande sur un week-end (aucun jour ouvré) doit lever BusinessRuleException")
    void create_onlyWeekend_shouldThrow() {
        createDTO.setStartDate(LocalDate.of(2026, 11, 7)); // Samedi
        createDTO.setEndDate(LocalDate.of(2026, 11, 8));   // Dimanche
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        assertThatThrownBy(() -> leaveRequestService.create(5L, createDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aucun jour ouvré");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Demande chevauchant une autre demande doit lever ConflictException")
    void create_overlapping_shouldThrowConflict() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.hasOverlappingLeave(eq(5L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> leaveRequestService.create(5L, createDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("chevauche");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Refus automatique si la demande dépasse le solde disponible")
    void create_insufficientBalance_shouldThrowBusinessRule() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.hasOverlappingLeave(eq(5L), any(), any())).thenReturn(false);
        // Solde restant : 3 jours, alors que la demande est de 5 jours
        when(leaveBalanceService.getRemainingBalance(5L, 1L, 2026)).thenReturn(3);

        assertThatThrownBy(() -> leaveRequestService.create(5L, createDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Solde insuffisant");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Annulation réussie par l'employé concerné")
    void cancel_byEmployee_shouldSucceed() {
        leaveRequest.setStatus(LeaveStatus.EN_ATTENTE);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        responseDTO.setStatus(LeaveStatus.ANNULEE);
        when(leaveRequestMapper.toResponseDTO(any(LeaveRequest.class))).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.cancel(10L, 5L);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.ANNULEE);
        verify(leaveRequestRepository).save(leaveRequest);
    }

    @Test
    @DisplayName("Tentative d'annulation par un autre employé doit lever BusinessRuleException")
    void cancel_byAnotherEmployee_shouldThrow() {
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.cancel(10L, 99L)) // id 99 != 5
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Seul l'employé concerné");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tentative d'annulation d'une demande déjà traitée doit lever BusinessRuleException")
    void cancel_alreadyApproved_shouldThrow() {
        leaveRequest.setStatus(LeaveStatus.APPROUVEE);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.cancel(10L, 5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("EN_ATTENTE");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation d'une demande EN_ATTENTE par le manager")
    void approve_shouldSucceed() {
        leaveRequest.setStatus(LeaveStatus.EN_ATTENTE);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));
        when(leaveBalanceService.getRemainingBalance(5L, 1L, 2026)).thenReturn(15);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        responseDTO.setStatus(LeaveStatus.APPROUVEE);
        when(leaveRequestMapper.toResponseDTO(any(LeaveRequest.class))).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.approve(10L);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.APPROUVEE);
        verify(leaveRequestRepository).save(leaveRequest);
    }

    @Test
    @DisplayName("Validation d'une demande déjà refusée doit lever BusinessRuleException")
    void approve_alreadyRejected_shouldThrow() {
        leaveRequest.setStatus(LeaveStatus.REFUSEE);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.approve(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("EN_ATTENTE");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Refus d'une demande avec commentaire obligatoire par le manager")
    void reject_withComment_shouldSucceed() {
        leaveRequest.setStatus(LeaveStatus.EN_ATTENTE);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        responseDTO.setStatus(LeaveStatus.REFUSEE);
        responseDTO.setComment("Effectif insuffisant sur cette période");
        when(leaveRequestMapper.toResponseDTO(any(LeaveRequest.class))).thenReturn(responseDTO);

        LeaveDecisionDTO decision = new LeaveDecisionDTO("Effectif insuffisant sur cette période");
        LeaveRequestResponseDTO result = leaveRequestService.reject(10L, decision);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.REFUSEE);
        verify(leaveRequestRepository).save(leaveRequest);
    }

    @Test
    @DisplayName("Refus d'une demande sans commentaire doit lever BusinessRuleException")
    void reject_withoutComment_shouldThrow() {
        leaveRequest.setStatus(LeaveStatus.EN_ATTENTE);
        when(leaveRequestRepository.findById(10L)).thenReturn(Optional.of(leaveRequest));

        LeaveDecisionDTO decisionEmpty = new LeaveDecisionDTO("   ");

        assertThatThrownBy(() -> leaveRequestService.reject(10L, decisionEmpty))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("commentaire");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lister les demandes d'un employé")
    void getEmployeeLeaves_shouldReturnList() {
        when(employeeRepository.existsById(5L)).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(5L)).thenReturn(List.of(leaveRequest));
        when(leaveRequestMapper.toResponseDTO(leaveRequest)).thenReturn(responseDTO);

        List<LeaveRequestResponseDTO> list = leaveRequestService.getEmployeeLeaves(5L);

        assertThat(list).hasSize(1);
    }

    @Test
    @DisplayName("Lister les demandes en attente pour un manager")
    void getPendingRequestsForManager_shouldReturnList() {
        when(employeeRepository.existsById(2L)).thenReturn(true); // manager id
        when(leaveRequestRepository.findByEmployeeManagerIdAndStatus(2L, LeaveStatus.EN_ATTENTE)).thenReturn(List.of(leaveRequest));
        when(leaveRequestMapper.toResponseDTO(leaveRequest)).thenReturn(responseDTO);

        List<LeaveRequestResponseDTO> list = leaveRequestService.getPendingRequestsForManager(2L);

        assertThat(list).hasSize(1);
    }
}
