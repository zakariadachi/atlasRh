package ma.atlasrh.service;

import ma.atlasrh.dto.EmployeeLeaveBalanceDTO;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.entity.LeaveType;
import ma.atlasrh.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    private Employee employee;
    private LeaveType annualLeave;
    private LeaveType exceptionalLeave;

    @BeforeEach
    void setUp() {
        employee = new Employee("ATL-001", "Karim", "Bennani", "karim@atlasrh.ma", "Directeur", LocalDate.of(2020, 1, 1));
        employee.setId(1L);

        annualLeave = new LeaveType("Annuel", "Congé annuel payé", 22);
        annualLeave.setId(1L);

        exceptionalLeave = new LeaveType("Exceptionnel", "Congé exceptionnel", 10);
        exceptionalLeave.setId(2L);
    }

    @Test
    @DisplayName("Calcul du solde restant : 22 annuels - 5 approuvés = 17 restants")
    void getRemainingBalance_shouldCalculateCorrectly() {
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(annualLeave));
        when(leaveRequestRepository.countApprovedDaysByYear(1L, 1L, 2026)).thenReturn(5);

        int remaining = leaveBalanceService.getRemainingBalance(1L, 1L, 2026);

        assertThat(remaining).isEqualTo(17);
    }

    @Test
    @DisplayName("Calcul du solde restant avec 0 jour approuvé = total annuel")
    void getRemainingBalance_zeroApproved_shouldReturnAnnualDays() {
        when(leaveTypeRepository.findById(2L)).thenReturn(Optional.of(exceptionalLeave));
        when(leaveRequestRepository.countApprovedDaysByYear(1L, 2L, 2026)).thenReturn(0);

        int remaining = leaveBalanceService.getRemainingBalance(1L, 2L, 2026);

        assertThat(remaining).isEqualTo(10);
    }

    @Test
    @DisplayName("Calcul du solde avec type inexistant doit lever ResourceNotFoundException")
    void getRemainingBalance_typeNotFound_shouldThrow() {
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveBalanceService.getRemainingBalance(1L, 99L, 2026))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Consultation de tous les soldes de l'employé avec succès")
    void getEmployeeBalances_shouldReturnAllBalances() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveTypeRepository.findAll()).thenReturn(List.of(annualLeave, exceptionalLeave));
        when(leaveRequestRepository.countApprovedDaysByYear(1L, 1L, 2026)).thenReturn(5);
        when(leaveRequestRepository.countApprovedDaysByYear(1L, 2L, 2026)).thenReturn(2);

        EmployeeLeaveBalanceDTO result = leaveBalanceService.getEmployeeBalances(1L, 2026);

        assertThat(result.getEmployeeId()).isEqualTo(1L);
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getBalances()).hasSize(2);

        // Vérification solde annuel
        assertThat(result.getBalances().get(0).getAnnualDays()).isEqualTo(22);
        assertThat(result.getBalances().get(0).getApprovedDays()).isEqualTo(5);
        assertThat(result.getBalances().get(0).getRemainingBalance()).isEqualTo(17);

        // Vérification solde exceptionnel
        assertThat(result.getBalances().get(1).getAnnualDays()).isEqualTo(10);
        assertThat(result.getBalances().get(1).getApprovedDays()).isEqualTo(2);
        assertThat(result.getBalances().get(1).getRemainingBalance()).isEqualTo(8);
    }

    @Test
    @DisplayName("Consultation des soldes pour employé inexistant doit lever ResourceNotFoundException")
    void getEmployeeBalances_employeeNotFound_shouldThrow() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveBalanceService.getEmployeeBalances(99L, 2026))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
