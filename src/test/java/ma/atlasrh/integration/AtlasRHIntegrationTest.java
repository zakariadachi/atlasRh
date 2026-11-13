
package ma.atlasrh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.atlasrh.entity.LeaveRequest;
import ma.atlasrh.repository.DepartmentRepository;
import ma.atlasrh.repository.EmployeeRepository;
import ma.atlasrh.repository.LeaveRequestRepository;
import ma.atlasrh.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.LeaveRequestCreateDTO;
import ma.atlasrh.entity.Department;
import ma.atlasrh.entity.LeaveType;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtlasRHIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();
        leaveTypeRepository.deleteAll();
    }


    @Test
    @DisplayName("Créer un employé, créer une demande de congé et l'approuver")
    void shouldCreateEmployeeAndApproveLeaveRequest() throws Exception {

        // GIVEN : préparer les données nécessaires
        Department department = departmentRepository.save(
                new Department("IT", "Informatique")
        );

        LeaveType leaveType = leaveTypeRepository.save(
                new LeaveType("Congé annuel", "Congé annuel payé", 22)
        );

        EmployeeCreateDTO employeeDTO = new EmployeeCreateDTO();
        employeeDTO.setMatricule("ATL-INT-001");
        employeeDTO.setFirstName("Amine");
        employeeDTO.setLastName("Benali");
        employeeDTO.setEmail("amine.integration@test.com");
        employeeDTO.setPosition("Développeur");
        employeeDTO.setHireDate(LocalDate.of(2025, 1, 1));
        employeeDTO.setDepartmentId(department.getId());

        // WHEN 1 : créer l'employé
        MvcResult employeeResult = mockMvc.perform(
                        post("/api/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(employeeDTO))
                )
                .andExpect(status().isCreated())
                .andReturn();

        long employeeId = objectMapper
                .readTree(employeeResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        // WHEN 2 : créer une demande de congé
        LeaveRequestCreateDTO leaveDTO = new LeaveRequestCreateDTO(
                leaveType.getId(),
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 6),
                "Congé personnel"
        );

        MvcResult leaveResult = mockMvc.perform(
                        post("/api/employees/" + employeeId + "/leave-requests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(leaveDTO))
                )
                .andExpect(status().isCreated())
                .andReturn();

        Long leaveRequestId = objectMapper
                .readTree(leaveResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        // WHEN 3 : approuver la demande
        mockMvc.perform(
                        patch("/api/leave-requests/" + leaveRequestId + "/approve")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROUVEE"));

        // THEN : vérifier le résultat directement en base
        LeaveRequest savedRequest = leaveRequestRepository
                .findById(leaveRequestId)
                .orElseThrow();

        assertThat(savedRequest.getStatus().name())
                .isEqualTo("APPROUVEE");
    }



    @Test
    @DisplayName("Refuser la suppression d'un département qui contient des employés")
    void shouldRejectDepartmentDeletionWhenEmployeesExist() throws Exception {

        // GIVEN : créer un département
        Department department = departmentRepository.save(
                new Department("RH", "Ressources humaines")
        );

        // Créer un employé rattaché à ce département
        EmployeeCreateDTO employeeDTO = new EmployeeCreateDTO();
        employeeDTO.setMatricule("ATL-INT-002");
        employeeDTO.setFirstName("Sara");
        employeeDTO.setLastName("Alami");
        employeeDTO.setEmail("sara.integration@test.com");
        employeeDTO.setPosition("Responsable RH");
        employeeDTO.setHireDate(LocalDate.of(2025, 1, 1));
        employeeDTO.setDepartmentId(department.getId());

        mockMvc.perform(
                        post("/api/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(employeeDTO))
                )
                .andExpect(status().isCreated());

        // WHEN : essayer de supprimer le département
        mockMvc.perform(
                        delete("/api/departments/" + department.getId())
                )
                // THEN : vérifier que la suppression est refusée
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Vérifier que le département existe toujours en base
        assertThat(departmentRepository.existsById(department.getId()))
                .isTrue();
    }

}
