package ma.atlasrh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.EmployeeResponseDTO;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.GlobalExceptionHandler;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@Import(GlobalExceptionHandler.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void shouldReturn201WhenEmployeeCreatedSuccessfully() throws Exception {
        EmployeeCreateDTO createDTO = new EmployeeCreateDTO();
        createDTO.setMatricule("EMP-001");
        createDTO.setFirstName("Zakaria");
        createDTO.setLastName("Dachi");
        createDTO.setEmail("zakaria@test.com");
        createDTO.setPosition("Developer");
        createDTO.setHireDate(LocalDate.of(2026, 1, 1));
        createDTO.setDepartmentId(1L);

        EmployeeResponseDTO responseDTO = new EmployeeResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setMatricule("EMP-001");
        responseDTO.setFirstName("Zakaria");
        responseDTO.setLastName("Dachi");
        responseDTO.setEmail("zakaria@test.com");

        when(employeeService.create(any(EmployeeCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.matricule").value("EMP-001"))
                .andExpect(jsonPath("$.email").value("zakaria@test.com"));
    }

    @Test
    void shouldReturn400WhenValidationFailsOnCreate() throws Exception {
        EmployeeCreateDTO invalidDTO = new EmployeeCreateDTO();
        // Manque tous les champs obligatoires (@NotBlank, @NotNull)

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        when(employeeService.findById(99L)).thenThrow(new ResourceNotFoundException("Employé avec l'id 99 introuvable"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employé avec l'id 99 introuvable"));
    }

    @Test
    void shouldReturn409WhenMatriculeAlreadyExists() throws Exception {
        EmployeeCreateDTO createDTO = new EmployeeCreateDTO();
        createDTO.setMatricule("EMP-DUPLICATE");
        createDTO.setFirstName("Zakaria");
        createDTO.setLastName("Dachi");
        createDTO.setEmail("duplicate@test.com");
        createDTO.setPosition("Developer");
        createDTO.setHireDate(LocalDate.of(2026, 1, 1));
        createDTO.setDepartmentId(1L);

        when(employeeService.create(any(EmployeeCreateDTO.class)))
                .thenThrow(new ConflictException("Le matricule 'EMP-DUPLICATE' est déjà utilisé"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Le matricule 'EMP-DUPLICATE' est déjà utilisé"));
    }
}
