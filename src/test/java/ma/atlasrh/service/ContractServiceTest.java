package ma.atlasrh.service;

import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.entity.Contract;
import ma.atlasrh.entity.ContractType;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.mapper.ContractMapper;
import ma.atlasrh.repository.ContractRepository;
import ma.atlasrh.repository.EmployeeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractService contractService;

    private ContractCreateDTO createDTO;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();

        createDTO = new ContractCreateDTO();
        createDTO.setType(ContractType.CDI);
        createDTO.setStartDate(LocalDate.of(2026, 1, 1));
        createDTO.setGrossSalary(BigDecimal.valueOf(5000));
    }

    // ── Contrat unique en cours ───────────────────────────────────────────────

    @Test
    void shouldCreateContractWhenNoOverlappingContractExists() {
        Contract contract = new Contract();
        contract.setEmployee(employee);
        ContractResponseDTO responseDTO = new ContractResponseDTO();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.hasOverlappingContract(1L, createDTO.getStartDate(), null)).thenReturn(false);
        when(contractMapper.toEntity(createDTO)).thenReturn(contract);
        when(contractRepository.save(contract)).thenReturn(contract);
        when(contractMapper.toResponseDTO(contract)).thenReturn(responseDTO);

        ContractResponseDTO result = contractService.create(1L, createDTO);

        assertSame(responseDTO, result);
        verify(contractRepository).save(contract);
    }

    @Test
    void shouldThrowConflictExceptionWhenOverlappingContractExists() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.hasOverlappingContract(1L, createDTO.getStartDate(), null)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> contractService.create(1L, createDTO)
        );

        assertTrue(exception.getMessage().contains("seul contrat actif"));
        verify(contractRepository, never()).save(any());
    }
}
