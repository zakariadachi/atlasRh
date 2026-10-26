package ma.atlasrh.service;

import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.entity.Contract;
import ma.atlasrh.entity.ContractType;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.ContractMapper;
import ma.atlasrh.repository.ContractRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractService contractService;

    private Employee employee;
    private Contract contract;
    private ContractCreateDTO createDTO;
    private ContractResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        employee = new Employee("ATL-001", "Karim", "Bennani", "karim@test.ma", "Dev", LocalDate.of(2020, 1, 1));
        employee.setId(1L);

        contract = new Contract(ContractType.CDI, LocalDate.of(2020, 1, 1), null, new BigDecimal("15000"), employee);
        contract.setId(1L);

        createDTO = new ContractCreateDTO();
        createDTO.setType(ContractType.CDI);
        createDTO.setStartDate(LocalDate.of(2020, 1, 1));
        createDTO.setGrossSalary(new BigDecimal("15000"));

        responseDTO = new ContractResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setType(ContractType.CDI);
    }

    @Test
    @DisplayName("Créer un contrat CDI avec succès")
    void create_cdi_shouldSucceed() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.hasOverlappingContract(eq(1L), any(), any())).thenReturn(false);
        when(contractMapper.toEntity(createDTO)).thenReturn(contract);
        when(contractRepository.save(contract)).thenReturn(contract);
        when(contractMapper.toResponseDTO(contract)).thenReturn(responseDTO);

        ContractResponseDTO result = contractService.create(1L, createDTO);

        assertThat(result.getType()).isEqualTo(ContractType.CDI);
        verify(contractRepository).save(contract);
    }

    @Test
    @DisplayName("Créer un contrat CDD sans date de fin doit lever BusinessRuleException")
    void create_cddWithoutEndDate_shouldThrow() {
        createDTO.setType(ContractType.CDD);
        createDTO.setEndDate(null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> contractService.create(1L, createDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");

        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer un contrat STAGE sans date de fin doit lever BusinessRuleException")
    void create_stageWithoutEndDate_shouldThrow() {
        createDTO.setType(ContractType.STAGE);
        createDTO.setEndDate(null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> contractService.create(1L, createDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("date de fin");
    }

    @Test
    @DisplayName("Créer un contrat avec date de fin avant date de début doit lever BusinessRuleException")
    void create_endDateBeforeStartDate_shouldThrow() {
        createDTO.setType(ContractType.CDD);
        createDTO.setStartDate(LocalDate.of(2024, 6, 1));
        createDTO.setEndDate(LocalDate.of(2024, 1, 1));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> contractService.create(1L, createDTO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("antérieure");
    }

    @Test
    @DisplayName("Créer un second contrat en cours pour un employé doit lever ConflictException")
    void create_overlappingContract_shouldThrowConflict() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.hasOverlappingContract(eq(1L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> contractService.create(1L, createDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("contrat");

        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer un contrat pour un employé inexistant doit lever ResourceNotFoundException")
    void create_employeeNotFound_shouldThrow() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.create(99L, createDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employé");
    }

    @Test
    @DisplayName("Terminer un contrat en cours avec succès")
    void terminate_shouldSucceed() {
        // Contrat CDI sans date de fin
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenReturn(contract);
        when(contractMapper.toResponseDTO(any())).thenReturn(responseDTO);

        ContractResponseDTO result = contractService.terminate(1L);

        assertThat(result).isNotNull();
        verify(contractRepository).save(any());
    }

    @Test
    @DisplayName("Terminer un contrat déjà terminé doit lever BusinessRuleException")
    void terminate_alreadyTerminated_shouldThrow() {
        contract.setEndDate(LocalDate.of(2020, 6, 1));
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> contractService.terminate(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà terminé");
    }

    @Test
    @DisplayName("Terminer un contrat inexistant doit lever ResourceNotFoundException")
    void terminate_notFound_shouldThrow() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.terminate(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Consulter un contrat inexistant doit lever ResourceNotFoundException")
    void findById_notFound_shouldThrow() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Historique des contrats d'un employé inexistant doit lever ResourceNotFoundException")
    void getHistory_employeeNotFound_shouldThrow() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> contractService.getContractHistory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Historique des contrats retourne la liste ordonnée")
    void getHistory_shouldReturnList() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(contractRepository.findByEmployeeIdOrderByStartDateDesc(1L)).thenReturn(List.of(contract));
        when(contractMapper.toResponseDTO(contract)).thenReturn(responseDTO);

        List<ContractResponseDTO> result = contractService.getContractHistory(1L);

        assertThat(result).hasSize(1);
    }
}
