package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.atlasrh.dto.EmployeeLeaveBalanceDTO;
import ma.atlasrh.exception.ErrorResponse;
import ma.atlasrh.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees/{employeeId}/leave-balance")
@Tag(name = "Soldes de congés", description = "Gestion et consultation des soldes de congés des collaborateurs (US6)")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping
    @Operation(summary = "Consulter le solde de congés d'un employé",
            description = "Retourne le solde des congés disponible par type pour une année donnée (par défaut l'année courante). "
                    + "Formule : Solde = jours annuels du type - jours approuvés de l'année.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solde de congés calculé avec succès"),
            @ApiResponse(responseCode = "404", description = "Employé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeLeaveBalanceDTO> getBalances(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long employeeId,
            @Parameter(description = "Année cible (ex: 2026)") @RequestParam(required = false) Integer year) {

        return ResponseEntity.ok(leaveBalanceService.getEmployeeBalances(employeeId, year));
    }
}
