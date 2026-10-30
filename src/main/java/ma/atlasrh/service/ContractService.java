package ma.atlasrh.service;

import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.dto.ContractUpdateDTO;
import ma.atlasrh.dto.PageResponseDTO;
import ma.atlasrh.entity.Contract;
import ma.atlasrh.entity.ContractType;
import ma.atlasrh.entity.Employee;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.mapper.ContractMapper;
import ma.atlasrh.repository.ContractRepository;
import ma.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractMapper contractMapper;

    public ContractService(
            ContractRepository contractRepository,
            EmployeeRepository employeeRepository,
            ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.contractMapper = contractMapper;
    }

    public ContractResponseDTO create(Long employeeId, ContractCreateDTO dto) {
        log.info("Création d'un contrat {} pour l'employé id={}",
                dto.getType(), employeeId);

        // Vérifier l'existence de l'employé
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé avec l'id " + employeeId + " introuvable"));

        // Vérifier que CDD et STAGE ont une date de fin
        if ((dto.getType() == ContractType.CDD
                || dto.getType() == ContractType.STAGE)
                && dto.getEndDate() == null) {

            throw new BusinessRuleException(
                    "Un contrat de type " + dto.getType()
                            + " doit obligatoirement avoir une date de fin");
        }

        // Vérifier que la date de fin n'est pas antérieure à la date de début
        if (dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {

            throw new BusinessRuleException(
                    "La date de fin ne peut pas être antérieure à la date de début");
        }

        // Vérifier le chevauchement avec des contrats existants
        if (contractRepository.hasOverlappingContract(
                employeeId, dto.getStartDate(), dto.getEndDate())) {

            throw new ConflictException(
                    "L'employé possède déjà un contrat qui chevauche la période demandée. "
                            + "Un employé ne peut avoir qu'un seul contrat actif à la fois.");
        }

        // Créer le contrat
        Contract contract = contractMapper.toEntity(dto);
        contract.setEmployee(employee);
        contract = contractRepository.save(contract);

        log.info("Contrat créé avec l'id {} pour l'employé id={}",
                contract.getId(), employeeId);

        return contractMapper.toResponseDTO(contract);
    }

    /**
     * Termine un contrat en fixant sa date de fin.
     * Si aujourd'hui est antérieur à la date de début,
     * la date de fin est fixée à la date de début.
     */
    public ContractResponseDTO terminate(Long contractId) {
        log.info("Terminaison du contrat id={}", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrat avec l'id " + contractId + " introuvable"));

        // Vérifier que le contrat n'est pas déjà terminé
        LocalDate today = LocalDate.now();

        if (contract.getEndDate() != null
                && contract.getEndDate().isBefore(today)) {

            throw new BusinessRuleException(
                    "Ce contrat est déjà terminé (date de fin : "
                            + contract.getEndDate() + ")");
        }

        // Fixer la date de fin
        LocalDate endDate = today.isBefore(contract.getStartDate())
                ? contract.getStartDate()
                : today;

        contract.setEndDate(endDate);
        contract = contractRepository.save(contract);

        log.info("Contrat id={} terminé avec date de fin {}",
                contractId, endDate);

        return contractMapper.toResponseDTO(contract);
    }

    @Transactional(readOnly = true)
    public ContractResponseDTO findById(Long contractId) {
        log.debug("Recherche du contrat id={}", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrat avec l'id " + contractId + " introuvable"));

        return contractMapper.toResponseDTO(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractHistory(Long employeeId) {
        log.debug("Historique des contrats de l'employé id={}", employeeId);

        // Vérifier l'existence de l'employé
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(
                    "Employé avec l'id " + employeeId + " introuvable");
        }

        return contractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId)
                .stream()
                .map(contractMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ContractResponseDTO update(Long contractId, ContractUpdateDTO dto) {
        log.info("Mise à jour du contrat id={}", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrat avec l'id " + contractId + " introuvable"));

        // Vérifier que CDD et STAGE ont une date de fin
        if ((dto.getType() == ContractType.CDD
                || dto.getType() == ContractType.STAGE)
                && dto.getEndDate() == null) {

            throw new BusinessRuleException(
                    "Un contrat de type " + dto.getType()
                            + " doit obligatoirement avoir une date de fin");
        }

        // Vérifier que la date de fin n'est pas antérieure à la date de début
        if (dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {

            throw new BusinessRuleException(
                    "La date de fin ne peut pas être antérieure à la date de début");
        }

        // Vérifier le chevauchement avec les autres contrats de l'employé
        Long employeeId = contract.getEmployee().getId();
        if (contractRepository.hasOverlappingContractExcluding(
                employeeId, contractId, dto.getStartDate(), dto.getEndDate())) {

            throw new ConflictException(
                    "Ce contrat chevauche un autre contrat existant pour cet employé.");
        }

        contractMapper.updateEntityFromDTO(dto, contract);
        contract = contractRepository.save(contract);

        log.info("Contrat id={} mis à jour", contractId);
        return contractMapper.toResponseDTO(contract);
    }

    public void delete(Long contractId) {
        log.info("Suppression du contrat id={}", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrat avec l'id " + contractId + " introuvable"));

        LocalDate today = LocalDate.now();
        boolean isActive = contract.getStartDate().compareTo(today) <= 0
                && (contract.getEndDate() == null || !contract.getEndDate().isBefore(today));

        if (isActive) {
            throw new ConflictException(
                    "Impossible de supprimer un contrat en cours. Il doit être terminé au préalable.");
        }

        contractRepository.delete(contract);
        log.info("Contrat id={} supprimé", contractId);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ContractResponseDTO> findAll(Pageable pageable) {
        log.debug("Listing de tous les contrats paginés");

        Page<Contract> page = contractRepository.findAll(pageable);
        List<ContractResponseDTO> content = page.getContent().stream()
                .map(contractMapper::toResponseDTO)
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
}