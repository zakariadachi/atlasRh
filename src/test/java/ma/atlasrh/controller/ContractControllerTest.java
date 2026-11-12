package ma.atlasrh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.entity.ContractType;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.GlobalExceptionHandler;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.service.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContractController.class)
@Import(GlobalExceptionHandler.class)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ContractService contractService;

    @Test
    void shouldReturn201WhenContractCreatedSuccessfully() throws Exception {
        ContractCreateDTO createDTO = new ContractCreateDTO();
        createDTO.setType(ContractType.CDI);
        createDTO.setStartDate(LocalDate.of(2026, 1, 1));
        createDTO.setGrossSalary(BigDecimal.valueOf(8000));

        ContractResponseDTO responseDTO = new ContractResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setType(ContractType.CDI);
        responseDTO.setStartDate(LocalDate.of(2026, 1, 1));
        responseDTO.setGrossSalary(BigDecimal.valueOf(8000));
        responseDTO.setEmployeeId(1L);

        when(contractService.create(eq(1L), any(ContractCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/employees/1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("CDI"))
                .andExpect(jsonPath("$.employeeId").value(1));
    }

    @Test
    void shouldReturn400WhenSalaryIsNegativeOnCreate() throws Exception {
        ContractCreateDTO invalidDTO = new ContractCreateDTO();
        invalidDTO.setType(ContractType.CDI);
        invalidDTO.setStartDate(LocalDate.of(2026, 1, 1));
        invalidDTO.setGrossSalary(BigDecimal.valueOf(-100)); // invalide car @Positive

        mockMvc.perform(post("/api/employees/1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void shouldReturn404WhenContractNotFound() throws Exception {
        when(contractService.findById(99L)).thenThrow(new ResourceNotFoundException("Contrat avec l'id 99 introuvable"));

        mockMvc.perform(get("/api/contracts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Contrat avec l'id 99 introuvable"));
    }

    @Test
    void shouldReturn409WhenOverlappingContractOnCreate() throws Exception {
        ContractCreateDTO createDTO = new ContractCreateDTO();
        createDTO.setType(ContractType.CDI);
        createDTO.setStartDate(LocalDate.of(2026, 1, 1));
        createDTO.setGrossSalary(BigDecimal.valueOf(8000));

        when(contractService.create(eq(1L), any(ContractCreateDTO.class)))
                .thenThrow(new ConflictException("L'employé possède déjà un contrat qui chevauche la période"));

        mockMvc.perform(post("/api/employees/1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("L'employé possède déjà un contrat qui chevauche la période"));
    }
}

