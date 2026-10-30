package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.EmployeeResponseDTO;
import ma.atlasrh.dto.EmployeeUpdateDTO;
import ma.atlasrh.dto.PageResponseDTO;
import ma.atlasrh.exception.ErrorResponse;
import ma.atlasrh.service.EmployeeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employés", description = "Gestion des employés de l'entreprise")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @Operation(summary = "Créer un employé",
            description = "Crée un nouvel employé. Le matricule et l'email doivent être uniques. Le département doit exister.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employé créé avec succès",
                    content = @Content(schema = @Schema(implementation = EmployeeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Données de validation invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Département ou manager introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Matricule ou email déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> create(@Valid @RequestBody EmployeeCreateDTO dto) {
        EmployeeResponseDTO created = employeeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Lister et rechercher les employés",
            description = "Retourne une liste paginée d'employés avec possibilité de filtrer par nom et/ou département. "
                    + "Le tri par défaut est par nom de famille croissant.")
    @ApiResponse(responseCode = "200", description = "Liste paginée d'employés")
    public ResponseEntity<PageResponseDTO<EmployeeResponseDTO>> search(
            @Parameter(description = "Nom ou prénom à rechercher") @RequestParam(required = false) String name,
            @Parameter(description = "Identifiant du département") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "lastName") String sortBy,
            @Parameter(description = "Direction du tri (asc/desc)") @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(employeeService.search(name, departmentId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un employé", description = "Retourne un employé par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employé trouvé"),
            @ApiResponse(responseCode = "404", description = "Employé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> findById(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un employé",
            description = "Met à jour les informations d'un employé. Le matricule n'est pas modifiable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employé mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données de validation invalides ou règle métier violée",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employé, département ou manager introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email déjà utilisé",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponseDTO> update(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @GetMapping("/{id}/team")
    @Operation(summary = "Consulter l'équipe d'un manager",
            description = "Retourne la liste des employés qui ont cet employé comme manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Équipe du manager"),
            @ApiResponse(responseCode = "404", description = "Manager introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<EmployeeResponseDTO>> getTeam(
            @Parameter(description = "Identifiant du manager") @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getTeam(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un employé",
            description = "Supprime un employé selon les règles métier. Impossible s'il a des contrats ou encadre d'autres collaborateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Employé supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Employé introuvable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "L'employé possède des contrats ou encadre des collaborateurs",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant de l'employé") @PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
