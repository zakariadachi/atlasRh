package ma.atlasrh.service;

import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.EmployeeResponseDTO;
import ma.atlasrh.dto.EmployeeUpdateDTO;
import ma.atlasrh.dto.PageResponseDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ContractRepository contractRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           ContractRepository contractRepository,
                           LeaveRequestRepository leaveRequestRepository,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.contractRepository = contractRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeMapper = employeeMapper;
    }

    public EmployeeResponseDTO create(EmployeeCreateDTO dto) {
        log.info("Création de l'employé : {} {}", dto.getFirstName(), dto.getLastName());

        // Vérification unicité du matricule
        if (employeeRepository.existsByMatricule(dto.getMatricule())) {
            throw new ConflictException(
                    "Le matricule '" + dto.getMatricule() + "' est déjà utilisé");
        }

        // Vérification unicité de l'email
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException(
                    "L'email '" + dto.getEmail() + "' est déjà utilisé");
        }

        // Résolution du département
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département avec l'id " + dto.getDepartmentId() + " introuvable"));

        // Mapper les champs simples
        Employee employee = employeeMapper.toEntity(dto);
        employee.setDepartment(department);

        // Résolution du manager
        if (dto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Manager (employé) avec l'id " + dto.getManagerId() + " introuvable"));
            employee.setManager(manager);
        }

        employee = employeeRepository.save(employee);
        log.info("Employé créé avec l'id {}", employee.getId());

        return employeeMapper.toResponseDTO(employee);
    }

    public EmployeeResponseDTO update(Long id, EmployeeUpdateDTO dto) {
        log.info("Mise à jour de l'employé id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé avec l'id " + id + " introuvable"));

        // Vérification unicité de l'email (exclure l'employé courant)
        if (employeeRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ConflictException(
                    "L'email '" + dto.getEmail() + "' est déjà utilisé");
        }

        // Résolution du département
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Département avec l'id " + dto.getDepartmentId() + " introuvable"));

        // Mapper les champs simples
        employeeMapper.updateEntityFromDTO(dto, employee);
        employee.setDepartment(department);

        // Résolution du manager
        if (dto.getManagerId() != null) {
            if (dto.getManagerId().equals(id)) {
                throw new BusinessRuleException(
                        "Un employé ne peut pas être son propre manager");
            }

            Employee manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Manager (employé) avec l'id " + dto.getManagerId() + " introuvable"));

            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        employee = employeeRepository.save(employee);

        return employeeMapper.toResponseDTO(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponseDTO findById(Long id) {
        log.debug("Recherche de l'employé id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé avec l'id " + id + " introuvable"));

        return employeeMapper.toResponseDTO(employee);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<EmployeeResponseDTO> search(
            String name, Long departmentId, Pageable pageable) {
        log.debug("Recherche d'employés - nom={}, departmentId={}, page={}",
                name, departmentId, pageable);

        Page<Employee> page = employeeRepository.search(name, departmentId, pageable);

        List<EmployeeResponseDTO> content = page.getContent().stream()
                .map(employeeMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getTeam(Long managerId) {
        log.debug("Recherche de l'équipe du manager id={}", managerId);

        // Vérifier que le manager existe
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException(
                    "Employé (manager) avec l'id " + managerId + " introuvable");
        }

        return employeeRepository.findByManagerId(managerId).stream()
                .map(employeeMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        log.info("Suppression de l'employé id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé avec l'id " + id + " introuvable"));

        // Règle métier : impossible de supprimer si l'employé possède des contrats
        if (contractRepository.existsByEmployeeId(id)) {
            throw new ConflictException(
                    "Impossible de supprimer l'employé '" + employee.getFirstName() + " " + employee.getLastName()
                            + "' car il possède des contrats enregistrés");
        }

        // Règle métier : impossible de supprimer si l'employé encadre d'autres collaborateurs
        if (employeeRepository.existsByManagerId(id)) {
            throw new ConflictException(
                    "Impossible de supprimer l'employé '" + employee.getFirstName() + " " + employee.getLastName()
                            + "' car il est le manager d'autres collaborateurs");
        }

        // Règle d'intégrité : impossible de supprimer si l'employé possède des demandes de congé
        if (leaveRequestRepository.existsByEmployeeId(id)) {
            throw new ConflictException(
                    "Impossible de supprimer l'employé '" + employee.getFirstName() + " " + employee.getLastName()
                            + "' car il possède des demandes de congé enregistrées");
        }

        employeeRepository.delete(employee);
        log.info("Employé id={} supprimé", id);
    }
}