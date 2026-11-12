package ma.atlasrh.service;

import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.EmployeeResponseDTO;
import ma.atlasrh.dto.EmployeeUpdateDTO;
import ma.atlasrh.entity.Department;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.mapper.EmployeeMapper;
import ma.atlasrh.repository.ContractRepository;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;

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
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        createDTO = new EmployeeCreateDTO();
        createDTO.setMatricule("EMP-001");
        createDTO.setFirstName("Zakaria");
        createDTO.setLastName("Dachi");
        createDTO.setEmail("zakaria@test.com");
        createDTO.setPosition("Developer");
        createDTO.setHireDate(LocalDate.of(2026, 1, 1));
        createDTO.setDepartmentId(1L);
    }

    // ── 1. Unicité du matricule ───────────────────────────────────────────────

    @Test
    void shouldCreateEmployeeWhenMatriculeIsAvailable() {
        Department department = new Department();
        Employee employee = new Employee();
        EmployeeResponseDTO responseDTO = new EmployeeResponseDTO();

        when(employeeRepository.existsByMatricule("EMP-001")).thenReturn(false);
        when(employeeRepository.existsByEmail("zakaria@test.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(createDTO)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponseDTO(employee)).thenReturn(responseDTO);

        EmployeeResponseDTO result = employeeService.create(createDTO);

        assertNotNull(result);
        assertSame(responseDTO, result);
        verify(employeeRepository).existsByMatricule("EMP-001");
        verify(employeeRepository).save(employee);
        verify(employeeMapper).toResponseDTO(employee);
    }

    @Test
    void shouldThrowConflictExceptionWhenMatriculeAlreadyExists() {
        when(employeeRepository.existsByMatricule("EMP-001")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> employeeService.create(createDTO)
        );

        assertEquals("Le matricule 'EMP-001' est déjà utilisé", exception.getMessage());
        verify(employeeRepository).existsByMatricule("EMP-001");
        verify(employeeRepository, never()).save(any(Employee.class));
        verifyNoInteractions(departmentRepository, employeeMapper);
    }

    // ── 2. Unicité de l'email ─────────────────────────────────────────────────

    @Test
    void shouldThrowConflictExceptionWhenEmailAlreadyExists() {
        when(employeeRepository.existsByMatricule("EMP-001")).thenReturn(false);
        when(employeeRepository.existsByEmail("zakaria@test.com")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> employeeService.create(createDTO)
        );

        assertEquals("L'email 'zakaria@test.com' est déjà utilisé", exception.getMessage());
        verify(employeeRepository).existsByEmail("zakaria@test.com");
        verify(employeeRepository, never()).save(any(Employee.class));
        verifyNoInteractions(departmentRepository, employeeMapper);
    }

    // ── 3. Règle d'auto-manager ───────────────────────────────────────────────

    @Test
    void shouldUpdateEmployeeWhenManagerIdIsDifferentFromEmployeeId() {
        EmployeeUpdateDTO updateDTO = buildUpdateDTO(2L);
        Employee employee = new Employee();
        Employee manager = new Employee();
        Department department = new Department();
        EmployeeResponseDTO responseDTO = new EmployeeResponseDTO();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("zakaria@test.com", 1L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponseDTO(employee)).thenReturn(responseDTO);

        EmployeeResponseDTO result = employeeService.update(1L, updateDTO);

        assertSame(responseDTO, result);
        verify(employeeRepository).save(employee);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenEmployeeIsOwnManager() {
        EmployeeUpdateDTO updateDTO = buildUpdateDTO(1L);
        Employee employee = new Employee();
        Department department = new Department();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("zakaria@test.com", 1L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> employeeService.update(1L, updateDTO)
        );

        assertEquals("Un employé ne peut pas être son propre manager", exception.getMessage());
        verify(employeeRepository, never()).save(any());
    }


    @Test
    void shouldCreateEmployeeWhenEmailIsAvailable() {
        Department department = new Department();
        Employee employee = new Employee();
        EmployeeResponseDTO responseDTO = new EmployeeResponseDTO();

        when(employeeRepository.existsByMatricule("EMP-001")).thenReturn(false);
        when(employeeRepository.existsByEmail("zakaria@test.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeMapper.toEntity(createDTO)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponseDTO(employee)).thenReturn(responseDTO);

        EmployeeResponseDTO result = employeeService.create(createDTO);

        assertNotNull(result);
        assertSame(responseDTO, result);

        verify(employeeRepository).existsByEmail("zakaria@test.com");
        verify(employeeRepository).save(employee);
        verify(employeeMapper).toResponseDTO(employee);
    }

    // helper

    private EmployeeUpdateDTO buildUpdateDTO(Long managerId) {
        EmployeeUpdateDTO dto = new EmployeeUpdateDTO();
        dto.setFirstName("Zakaria");
        dto.setLastName("Dachi");
        dto.setEmail("zakaria@test.com");
        dto.setPosition("Developer");
        dto.setHireDate(LocalDate.of(2026, 1, 1));
        dto.setDepartmentId(1L);
        dto.setManagerId(managerId);
        return dto;
    }



}
