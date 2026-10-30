package ma.atlasrh.service;

import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.EmployeeResponseDTO;
import ma.atlasrh.dto.EmployeeUpdateDTO;
import ma.atlasrh.entity.Department;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.EmployeeMapper;
import ma.atlasrh.repository.ContractRepository;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private Department department;
    private Employee employee;
    private EmployeeCreateDTO createDTO;
    private EmployeeResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        department = new Department("Développement", "Équipe dev");
        department.setId(1L);

        employee = new Employee("ATL-010", "Ahmed", "Benali", "ahmed@test.ma", "Développeur", LocalDate.of(2023, 1, 1));
        employee.setId(10L);
        employee.setDepartment(department);

        createDTO = new EmployeeCreateDTO();
        createDTO.setMatricule("ATL-010");
        createDTO.setFirstName("Ahmed");
        createDTO.setLastName("Benali");
        createDTO.setEmail("ahmed@test.ma");
        createDTO.setPosition("Développeur");
        createDTO.setHireDate(LocalDate.of(2023, 1, 1));
        createDTO.setDepartmentId(1L);

        responseDTO = new EmployeeResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setMatricule("ATL-010");
        responseDTO.setFirstName("Ahmed");
        responseDTO.setLastName("Benali");
    }

    @Test
    @DisplayName("Créer un employé avec succès")
    void create_shouldSucceed() {
        when(employeeRepository.existsByMatricule("ATL-010")).thenReturn(false);
        when(employeeRepository.existsByEmail("ahmed@test.ma")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(createDTO)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponseDTO(employee)).thenReturn(responseDTO);

        EmployeeResponseDTO result = employeeService.create(createDTO);

        assertThat(result.getMatricule()).isEqualTo("ATL-010");
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Créer un employé avec un matricule déjà utilisé doit lever ConflictException")
    void create_duplicateMatricule_shouldThrowConflict() {
        when(employeeRepository.existsByMatricule("ATL-010")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(createDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("matricule");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer un employé avec un email déjà utilisé doit lever ConflictException")
    void create_duplicateEmail_shouldThrowConflict() {
        when(employeeRepository.existsByMatricule("ATL-010")).thenReturn(false);
        when(employeeRepository.existsByEmail("ahmed@test.ma")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(createDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer un employé avec un département inexistant doit lever ResourceNotFoundException")
    void create_departmentNotFound_shouldThrow() {
        when(employeeRepository.existsByMatricule("ATL-010")).thenReturn(false);
        when(employeeRepository.existsByEmail("ahmed@test.ma")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Département");
    }

    @Test
    @DisplayName("Créer un employé avec un manager inexistant doit lever ResourceNotFoundException")
    void create_managerNotFound_shouldThrow() {
        createDTO.setManagerId(999L);
        when(employeeRepository.existsByMatricule("ATL-010")).thenReturn(false);
        when(employeeRepository.existsByEmail("ahmed@test.ma")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(createDTO)).thenReturn(employee);
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Manager");
    }

    @Test
    @DisplayName("Un employé ne peut pas être son propre manager")
    void update_selfManager_shouldThrowBusinessRule() {
        EmployeeUpdateDTO updateDTO = new EmployeeUpdateDTO();
        updateDTO.setFirstName("Ahmed");
        updateDTO.setLastName("Benali");
        updateDTO.setEmail("ahmed@test.ma");
        updateDTO.setPosition("Développeur");
        updateDTO.setHireDate(LocalDate.of(2023, 1, 1));
        updateDTO.setDepartmentId(1L);
        updateDTO.setManagerId(10L); // même id que l'employé

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("ahmed@test.ma", 10L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> employeeService.update(10L, updateDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("propre manager");
    }

    @Test
    @DisplayName("Consulter un employé inexistant doit lever ResourceNotFoundException")
    void findById_notFound_shouldThrow() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Consulter l'équipe d'un manager inexistant doit lever ResourceNotFoundException")
    void getTeam_managerNotFound_shouldThrow() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.getTeam(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Supprimer un employé sans contrat ni subordonné avec succès")
    void delete_shouldSucceed() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(contractRepository.existsByEmployeeId(10L)).thenReturn(false);
        when(employeeRepository.existsByManagerId(10L)).thenReturn(false);
        when(leaveRequestRepository.existsByEmployeeId(10L)).thenReturn(false);

        employeeService.delete(10L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    @DisplayName("Supprimer un employé inexistant doit lever ResourceNotFoundException")
    void delete_notFound_shouldThrow() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer un employé ayant des contrats doit lever ConflictException")
    void delete_withContracts_shouldThrowConflict() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(contractRepository.existsByEmployeeId(10L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.delete(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("contrats");

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer un employé manager doit lever ConflictException")
    void delete_isManager_shouldThrowConflict() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(contractRepository.existsByEmployeeId(10L)).thenReturn(false);
        when(employeeRepository.existsByManagerId(10L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.delete(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("manager");

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer un employé ayant des demandes de congé doit lever ConflictException")
    void delete_withLeaveRequests_shouldThrowConflict() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(contractRepository.existsByEmployeeId(10L)).thenReturn(false);
        when(employeeRepository.existsByManagerId(10L)).thenReturn(false);
        when(leaveRequestRepository.existsByEmployeeId(10L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.delete(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("demandes de congé");

        verify(employeeRepository, never()).delete(any());
    }
}
