package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.dto.ContractUpdateDTO;
import ma.atlasrh.dto.PageResponseDTO;
import ma.atlasrh.exception.ErrorResponse;
import ma.atlasrh.service.ContractService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Contrats", description = "Gestion des contrats des employés")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/employees/{employeeId}/contracts")
    @Operation(summary = "Créer un contrat",
            description = "Crée un nouveau contrat pour un employé. Un employé ne peut avoir qu'un seul contrat actif à la fois. "
                    + "Les contrats CDD et STAGE doivent avoir une date de fin.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrat créé avec succès",
                    content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou règle métier violée",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "L'employé a déjà un contrat actif sur cette période",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ContractResponseDTO> create(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long employeeId,
            @Valid @RequestBody ContractCreateDTO dto) {
        ContractResponseDTO created = contractService.create(employeeId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/employees/{employeeId}/contracts")
    @Operation(summary = "Historique des contrats",
            description = "Retourne l'historique complet des contrats d'un employé, triés du plus récent au plus ancien.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des contrats"),
            @ApiResponse(responseCode = "404", description = "Employé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ContractResponseDTO>> getHistory(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long employeeId) {
        return ResponseEntity.ok(contractService.getContractHistory(employeeId));
    }

    @GetMapping("/contracts/{id}")
    @Operation(summary = "Consulter un contrat", description = "Retourne un contrat par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrat trouvé"),
            @ApiResponse(responseCode = "404", description = "Contrat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ContractResponseDTO> findById(
            @Parameter(description = "Identifiant du contrat") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @PatchMapping("/contracts/{id}/terminate")
    @Operation(summary = "Terminer un contrat",
            description = "Termine un contrat en cours en fixant sa date de fin à la date du jour. "
                    + "Un contrat déjà terminé ne peut pas être terminé à nouveau.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrat terminé avec succès",
                    content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Le contrat est déjà terminé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contrat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ContractResponseDTO> terminate(
            @Parameter(description = "Identifiant du contrat") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.terminate(id));
    }

    @GetMapping("/contracts")
    @Operation(summary = "Lister les contrats",
            description = "Retourne la liste paginée de tous les contrats de l'entreprise.")
    @ApiResponse(responseCode = "200", description = "Liste paginée des contrats")
    public ResponseEntity<PageResponseDTO<ContractResponseDTO>> findAll(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "startDate") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(contractService.findAll(pageable));
    }

    @PutMapping("/contracts/{id}")
    @Operation(summary = "Modifier un contrat",
            description = "Met à jour les informations d'un contrat existant en vérifiant la cohérence des dates et des types.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrat mis à jour avec succès",
                    content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou règle métier violée",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contrat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Chevauchement avec un autre contrat",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ContractResponseDTO> update(
            @Parameter(description = "Identifiant du contrat") @PathVariable Long id,
            @Valid @RequestBody ContractUpdateDTO dto) {
        return ResponseEntity.ok(contractService.update(id, dto));
    }

    @DeleteMapping("/contracts/{id}")
    @Operation(summary = "Supprimer un contrat",
            description = "Supprime un contrat selon les règles métier. Impossible de supprimer un contrat en cours (actif).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrat supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Contrat introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Impossible de supprimer un contrat en cours",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant du contrat") @PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
