package ma.atlasrh.service;

import ma.atlasrh.dto.DepartmentRequestDTO;
import ma.atlasrh.dto.DepartmentResponseDTO;
import ma.atlasrh.entity.Department;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.DepartmentMapper;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private DepartmentRequestDTO requestDTO;
    private DepartmentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        department = new Department("Développement", "Équipe dev");
        department.setId(1L);

        requestDTO = new DepartmentRequestDTO("Développement", "Équipe dev");

        responseDTO = new DepartmentResponseDTO(1L, "Développement", "Équipe dev");
    }

    @Test
    @DisplayName("Créer un département avec succès")
    void create_shouldSucceed() {
        when(departmentRepository.existsByName("Développement")).thenReturn(false);
        when(departmentMapper.toEntity(requestDTO)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(department);
        when(departmentMapper.toResponseDTO(department)).thenReturn(responseDTO);

        DepartmentResponseDTO result = departmentService.create(requestDTO);

        assertThat(result.getName()).isEqualTo("Développement");
        verify(departmentRepository).save(department);
    }

    @Test
    @DisplayName("Créer un département avec un nom déjà existant doit lever ConflictException")
    void create_duplicateName_shouldThrowConflict() {
        when(departmentRepository.existsByName("Développement")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(requestDTO))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existe déjà");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modifier un département avec un nom déjà utilisé par un autre doit lever ConflictException")
    void update_duplicateName_shouldThrowConflict() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameAndIdNot("Développement", 1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.update(1L, requestDTO))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Modifier un département inexistant doit lever ResourceNotFoundException")
    void update_notFound_shouldThrow() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.update(99L, requestDTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Consulter un département par id avec succès")
    void findById_shouldSucceed() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentMapper.toResponseDTO(department)).thenReturn(responseDTO);

        DepartmentResponseDTO result = departmentService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Consulter un département inexistant doit lever ResourceNotFoundException")
    void findById_notFound_shouldThrow() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lister tous les départements")
    void findAll_shouldReturnList() {
        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(departmentMapper.toResponseDTO(department)).thenReturn(responseDTO);

        List<DepartmentResponseDTO> result = departmentService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Supprimer un département avec succès")
    void delete_shouldSucceed() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.delete(1L);

        verify(departmentRepository).delete(department);
    }

    @Test
    @DisplayName("Supprimer un département contenant des employés doit lever ConflictException")
    void delete_withEmployees_shouldThrowConflict() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("employés");

        verify(departmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Supprimer un département inexistant doit lever ResourceNotFoundException")
    void delete_notFound_shouldThrow() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
