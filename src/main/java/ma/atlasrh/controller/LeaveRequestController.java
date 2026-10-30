package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.LeaveDecisionDTO;
import ma.atlasrh.dto.LeaveRequestCreateDTO;
import ma.atlasrh.dto.LeaveRequestResponseDTO;
import ma.atlasrh.exception.ErrorResponse;
import ma.atlasrh.service.LeaveRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Demandes de congés", description = "Gestion des demandes de congés (US4 et US5)")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping("/employees/{employeeId}/leave-requests")
    @Operation(summary = "Déposer une demande de congé",
            description = "Permet à un employé de déposer une demande de congé (US5). "
                    + "Les jours ouvrés sont calculés du lundi au vendredi. La demande est refusée automatiquement si le solde est dépassé.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande de congé déposée avec succès",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dates incohérentes, pas de jour ouvré ou solde dépassé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employé ou type de congé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Chevauchement avec une autre demande active",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponseDTO> create(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long employeeId,
            @Valid @RequestBody LeaveRequestCreateDTO dto) {
        LeaveRequestResponseDTO created = leaveRequestService.create(employeeId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/employees/{employeeId}/leave-requests")
    @Operation(summary = "Consulter les demandes d'un employé",
            description = "Retourne l'historique complet des demandes de congé d'un employé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des demandes de l'employé"),
            @ApiResponse(responseCode = "404", description = "Employé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<LeaveRequestResponseDTO>> getEmployeeLeaves(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getEmployeeLeaves(employeeId));
    }

    @GetMapping("/leave-requests/{id}")
    @Operation(summary = "Consulter une demande de congé", description = "Retourne le détail d'une demande de congé par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande de congé trouvée"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponseDTO> findById(
            @Parameter(description = "Identifiant de la demande") @PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.findById(id));
    }

    @PatchMapping("/employees/{employeeId}/leave-requests/{id}/cancel")
    @Operation(summary = "Annuler une demande de congé",
            description = "Permet à l'employé d'annuler sa demande de congé tant qu'elle est au statut EN_ATTENTE (US5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande de congé annulée",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Demande déjà traitée ou tentative d'annulation par un tiers",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Demande introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponseDTO> cancel(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long employeeId,
            @Parameter(description = "Identifiant de la demande") @PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.cancel(id, employeeId));
    }

    @GetMapping("/managers/{managerId}/leave-requests/pending")
    @Operation(summary = "Consulter les demandes en attente de validation",
            description = "Permet à un manager de consulter les demandes au statut EN_ATTENTE de ses subordonnés directs (US4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demandes en attente de l'équipe"),
            @ApiResponse(responseCode = "404", description = "Manager introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<LeaveRequestResponseDTO>> getPendingRequestsForManager(
            @Parameter(description = "Identifiant du manager") @PathVariable Long managerId) {
        return ResponseEntity.ok(leaveRequestService.getPendingRequestsForManager(managerId));
    }

    @PatchMapping("/leave-requests/{id}/approve")
    @Operation(summary = "Valider une demande de congé",
            description = "Permet au manager d'approuver une demande de congé. Une décision est possible uniquement sur une demande EN_ATTENTE (US4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande de congé approuvée",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "La demande n'est pas au statut EN_ATTENTE ou solde insuffisant",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Demande introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponseDTO> approve(
            @Parameter(description = "Identifiant de la demande") @PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.approve(id));
    }

    @PatchMapping("/leave-requests/{id}/reject")
    @Operation(summary = "Refuser une demande de congé",
            description = "Permet au manager de refuser une demande de congé. Un commentaire expliquant le motif est obligatoire (US4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande de congé refusée",
                    content = @Content(schema = @Schema(implementation = LeaveRequestResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "La demande n'est pas EN_ATTENTE ou commentaire manquant",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Demande introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponseDTO> reject(
            @Parameter(description = "Identifiant de la demande") @PathVariable Long id,
            @Valid @RequestBody LeaveDecisionDTO decisionDTO) {
        return ResponseEntity.ok(leaveRequestService.reject(id, decisionDTO));
    }
}
