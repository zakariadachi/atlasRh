package ma.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.atlasrh.dto.LeaveTypeResponseDTO;
import ma.atlasrh.repository.LeaveTypeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leave-types")
@Tag(name = "Types de congés", description = "Consultation des types de congés de l'entreprise")
public class LeaveTypeController {

    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveTypeController(LeaveTypeRepository leaveTypeRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @GetMapping
    @Operation(summary = "Lister les types de congés", description = "Retourne la liste des types de congés configurés avec leur nombre de jours annuels.")
    @ApiResponse(responseCode = "200", description = "Liste des types de congé")
    public ResponseEntity<List<LeaveTypeResponseDTO>> findAll() {
        List<LeaveTypeResponseDTO> list = leaveTypeRepository.findAll().stream()
                .map(t -> new LeaveTypeResponseDTO(t.getId(), t.getName(), t.getDescription(), t.getDaysPerYear()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
