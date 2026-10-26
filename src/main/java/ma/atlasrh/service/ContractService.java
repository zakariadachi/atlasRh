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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service métier pour la gestion des contrats.
 *
 * <p><b>Définition de contrat en cours :</b></p>
 * Un contrat est considéré comme « en cours » si :
 * <ul>
 *   <li>Sa date de début est atteinte (startDate &lt;= aujourd'hui)</li>
 *   <li>Sa date de fin est absente (null) OU postérieure ou égale à aujourd'hui</li>
 * </ul>
 *
 * <p><b>Règle de terminaison :</b></p>
 * Terminer un contrat renseigne sa date de fin à la date du jour (LocalDate.now()).
 * Si la date du jour est antérieure à la date de début du contrat, la date de fin
 * est fixée à la date de début pour garantir la cohérence.
 */
@Service
@Transactional
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractMapper contractMapper;

    public ContractService(ContractRepository contractRepository,
                           EmployeeRepository employeeRepository,
                           ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.contractMapper = contractMapper;
    }

    public ContractResponseDTO create(Long employeeId, ContractCreateDTO dto) {
        log.info("Création d'un contrat {} pour l'employé id={}", dto.getType(), employeeId);

        // Vérifier l'existence de l'employé
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeeId));

        // Vérifier que CDD et STAGE ont une date de fin
        if ((dto.getType() == ContractType.CDD || dto.getType() == ContractType.STAGE)
                && dto.getEndDate() == null) {
            throw new BusinessRuleException("Un contrat de type " + dto.getType()
                    + " doit obligatoirement avoir une date de fin");
        }

        // Vérifier que la date de fin n'est pas antérieure à la date de début
        if (dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessRuleException("La date de fin ne peut pas être antérieure à la date de début");
        }

        // Vérifier le chevauchement avec des contrats existants
        if (contractRepository.hasOverlappingContract(employeeId, dto.getStartDate(), dto.getEndDate())) {
            throw new ConflictException("L'employé possède déjà un contrat qui chevauche la période demandée. "
                    + "Un employé ne peut avoir qu'un seul contrat actif à la fois.");
        }

        Contract contract = contractMapper.toEntity(dto);
        contract.setEmployee(employee);
        contract = contractRepository.save(contract);

        log.info("Contrat créé avec l'id {} pour l'employé id={}", contract.getId(), employeeId);

        return contractMapper.toResponseDTO(contract);
    }

    /**
     * Termine un contrat en cours en fixant sa date de fin.
     *
     * <p>La date de fin est fixée à la date du jour (LocalDate.now()).
     * Si la date du jour est antérieure à la date de début du contrat,
     * la date de fin est fixée à la date de début.</p>
     */
    public ContractResponseDTO terminate(Long contractId) {
        log.info("Terminaison du contrat id={}", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat", contractId));

        // Vérifier que le contrat n'est pas déjà terminé
        LocalDate today = LocalDate.now();
        if (contract.getEndDate() != null && contract.getEndDate().isBefore(today)) {
            throw new BusinessRuleException("Ce contrat est déjà terminé (date de fin : "
                    + contract.getEndDate() + ")");
        }

        // Fixer la date de fin
        LocalDate endDate = today.isBefore(contract.getStartDate()) ? contract.getStartDate() : today;
        contract.setEndDate(endDate);
        contract = contractRepository.save(contract);

        log.info("Contrat id={} terminé avec date de fin {}", contractId, endDate);

        return contractMapper.toResponseDTO(contract);
    }

    @Transactional(readOnly = true)
    public ContractResponseDTO findById(Long contractId) {
        log.debug("Recherche du contrat id={}", contractId);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat", contractId));

        return contractMapper.toResponseDTO(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getContractHistory(Long employeeId) {
        log.debug("Historique des contrats de l'employé id={}", employeeId);

        // Vérifier l'existence de l'employé
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé", employeeId);
        }

        return contractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId).stream()
                .map(contractMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
