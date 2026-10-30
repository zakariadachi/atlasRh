package ma.atlasrh.service;

import ma.atlasrh.dto.DepartmentRequestDTO;
import ma.atlasrh.dto.DepartmentResponseDTO;
import ma.atlasrh.entity.Department;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.DepartmentMapper;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository,
                             DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.departmentMapper = departmentMapper;
    }

    public DepartmentResponseDTO create(DepartmentRequestDTO dto) {
        log.info("Création du département : {}", dto.getName());

        if (departmentRepository.existsByName(dto.getName())) {
            throw new ConflictException(
                    "Un département avec le nom '" + dto.getName() + "' existe déjà");
        }

        Department department = departmentMapper.toEntity(dto);
        department = departmentRepository.save(department);

        log.info("Département créé avec l'id {}", department.getId());

        return departmentMapper.toResponseDTO(department);
    }

    public DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto) {
        log.info("Mise à jour du département id={}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département avec l'id " + id + " introuvable"));

        if (departmentRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new ConflictException(
                    "Un département avec le nom '" + dto.getName() + "' existe déjà");
        }

        departmentMapper.updateEntityFromDTO(dto, department);
        department = departmentRepository.save(department);

        return departmentMapper.toResponseDTO(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponseDTO findById(Long id) {
        log.debug("Recherche du département id={}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département avec l'id " + id + " introuvable"));

        return departmentMapper.toResponseDTO(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponseDTO> findAll() {
        log.debug("Listing de tous les départements");

        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        log.info("Suppression du département id={}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département avec l'id " + id + " introuvable"));

        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException(
                    "Impossible de supprimer le département '" + department.getName()
                            + "' car il contient encore des employés");
        }

        departmentRepository.delete(department);

        log.info("Département id={} supprimé", id);
    }
}