package ma.atlasrh.service;

import ma.atlasrh.dto.LeaveDecisionDTO;
import ma.atlasrh.dto.LeaveRequestCreateDTO;
import ma.atlasrh.dto.LeaveRequestResponseDTO;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.entity.LeaveStatus;
import ma.atlasrh.entity.LeaveType;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.mapper.LeaveRequestMapper;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import ma.atlasrh.repository.LeaveTypeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setFirstName("Zakaria");
        employee.setLastName("Dachi");

        leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setDaysPerYear(22);
    }

    // ── 1. Calcul des jours ouvrés ────────────────────────────────────────────

    @Test
    void shouldCreateLeaveRequestWhenDatesContainBusinessDays() {
        // lundi au vendredi = 5 jours ouvrés
        LeaveRequestCreateDTO dto = buildCreateDTO(
                LocalDate.of(2026, 1, 5),  // lundi
                LocalDate.of(2026, 1, 9)   // vendredi
        );
        LeaveRequest saved = buildLeaveRequest(employee, leaveType,
                dto.getStartDate(), dto.getEndDate(), 5);
        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.hasOverlappingLeave(1L, dto.getStartDate(), dto.getEndDate())).thenReturn(false);
        when(leaveBalanceService.getRemainingBalance(1L, 1L, 2026)).thenReturn(22);
        when(leaveRequestRepository.save(any())).thenReturn(saved);
        when(leaveRequestMapper.toResponseDTO(saved)).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.create(1L, dto);

        assertSame(responseDTO, result);
        verify(leaveRequestRepository).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenDatesAreWeekendOnly() {
        // samedi et dimanche = 0 jours ouvrés
        LeaveRequestCreateDTO dto = buildCreateDTO(
                LocalDate.of(2026, 1, 3),  // samedi
                LocalDate.of(2026, 1, 4)   // dimanche
        );

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> leaveRequestService.create(1L, dto)
        );

        assertTrue(exception.getMessage().contains("aucun jour ouvré"));
        verify(leaveRequestRepository, never()).save(any());
    }

    // ── 2. Solde de congés ────────────────────────────────────────────────────

    @Test
    void shouldCreateLeaveRequestWhenBalanceIsSufficient() {
        LeaveRequestCreateDTO dto = buildCreateDTO(
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 9)   // 5 jours ouvrés
        );
        LeaveRequest saved = buildLeaveRequest(employee, leaveType,
                dto.getStartDate(), dto.getEndDate(), 5);
        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.hasOverlappingLeave(1L, dto.getStartDate(), dto.getEndDate())).thenReturn(false);
        when(leaveBalanceService.getRemainingBalance(1L, 1L, 2026)).thenReturn(10);
        when(leaveRequestRepository.save(any())).thenReturn(saved);
        when(leaveRequestMapper.toResponseDTO(saved)).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.create(1L, dto);

        assertSame(responseDTO, result);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenBalanceIsInsufficient() {
        LeaveRequestCreateDTO dto = buildCreateDTO(
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 9)   // 5 jours ouvrés
        );

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveRequestRepository.hasOverlappingLeave(1L, dto.getStartDate(), dto.getEndDate())).thenReturn(false);
        when(leaveBalanceService.getRemainingBalance(1L, 1L, 2026)).thenReturn(2); // insuffisant

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> leaveRequestService.create(1L, dto)
        );

        assertTrue(exception.getMessage().contains("Solde insuffisant"));
        verify(leaveRequestRepository, never()).save(any());
    }

    // ── 3. Décision sur une demande (approve / reject) ────────────────────────

    @Test
    void shouldApproveLeaveRequestWhenStatusIsEnAttente() {
        LeaveRequest lr = buildLeaveRequest(employee, leaveType,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), 5);
        lr.setStatus(LeaveStatus.EN_ATTENTE);
        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));
        when(leaveBalanceService.getRemainingBalance(any(), any(), anyInt())).thenReturn(22);
        when(leaveRequestRepository.save(lr)).thenReturn(lr);
        when(leaveRequestMapper.toResponseDTO(lr)).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.approve(1L);

        assertSame(responseDTO, result);
        assertEquals(LeaveStatus.APPROUVEE, lr.getStatus());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenApprovingNonPendingRequest() {
        LeaveRequest lr = buildLeaveRequest(employee, leaveType,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), 5);
        lr.setStatus(LeaveStatus.APPROUVEE);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> leaveRequestService.approve(1L)
        );

        assertTrue(exception.getMessage().contains("EN_ATTENTE"));
    }

    @Test
    void shouldRejectLeaveRequestWhenCommentIsProvided() {
        LeaveRequest lr = buildLeaveRequest(employee, leaveType,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), 5);
        lr.setStatus(LeaveStatus.EN_ATTENTE);
        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(lr)).thenReturn(lr);
        when(leaveRequestMapper.toResponseDTO(lr)).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.reject(1L,
                new LeaveDecisionDTO("Période critique"));

        assertSame(responseDTO, result);
        assertEquals(LeaveStatus.REFUSEE, lr.getStatus());
        assertEquals("Période critique", lr.getComment());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenRejectingWithoutComment() {
        LeaveRequest lr = buildLeaveRequest(employee, leaveType,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), 5);
        lr.setStatus(LeaveStatus.EN_ATTENTE);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> leaveRequestService.reject(1L, new LeaveDecisionDTO(""))
        );

        assertTrue(exception.getMessage().contains("commentaire"));
    }

    // ── 4. Annulation ─────────────────────────────────────────────────────────

    @Test
    void shouldCancelLeaveRequestWhenStatusIsEnAttente() {
        LeaveRequest lr = buildLeaveRequest(employee, leaveType,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), 5);
        lr.setStatus(LeaveStatus.EN_ATTENTE);
        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();

        // l'employé a l'id null par défaut, on lui en donne un via réflexion
        setEmployeeId(employee, 1L);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));
        when(leaveRequestRepository.save(lr)).thenReturn(lr);
        when(leaveRequestMapper.toResponseDTO(lr)).thenReturn(responseDTO);

        LeaveRequestResponseDTO result = leaveRequestService.cancel(1L, 1L);

        assertSame(responseDTO, result);
        assertEquals(LeaveStatus.ANNULEE, lr.getStatus());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCancellingNonPendingRequest() {
        LeaveRequest lr = buildLeaveRequest(employee, leaveType,
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), 5);
        lr.setStatus(LeaveStatus.APPROUVEE);
        setEmployeeId(employee, 1L);

        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(lr));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> leaveRequestService.cancel(1L, 1L)
        );

        assertTrue(exception.getMessage().contains("EN_ATTENTE"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private LeaveRequestCreateDTO buildCreateDTO(LocalDate start, LocalDate end) {
        LeaveRequestCreateDTO dto = new LeaveRequestCreateDTO();
        dto.setLeaveTypeId(1L);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setReason("Test");
        return dto;
    }

    private LeaveRequest buildLeaveRequest(Employee emp, LeaveType lt,
                                           LocalDate start, LocalDate end, int days) {
        return new LeaveRequest(emp, lt, start, end, days, "Test");
    }

    private void setEmployeeId(Employee emp, Long id) {
        try {
            var field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(emp, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
