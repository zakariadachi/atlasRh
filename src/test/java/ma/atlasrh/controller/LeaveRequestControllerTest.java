package ma.atlasrh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.atlasrh.dto.LeaveRequestCreateDTO;
import ma.atlasrh.dto.LeaveRequestResponseDTO;
import ma.atlasrh.entity.LeaveStatus;
import ma.atlasrh.exception.BusinessRuleException;
import ma.atlasrh.exception.ConflictException;
import ma.atlasrh.exception.GlobalExceptionHandler;
import ma.atlasrh.exception.ResourceNotFoundException;
import ma.atlasrh.service.LeaveRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveRequestController.class)
@Import(GlobalExceptionHandler.class)
class LeaveRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @Test
    void shouldReturn201WhenLeaveRequestCreatedSuccessfully() throws Exception {
        LeaveRequestCreateDTO createDTO = new LeaveRequestCreateDTO();
        createDTO.setLeaveTypeId(1L);
        createDTO.setStartDate(LocalDate.of(2026, 1, 5));
        createDTO.setEndDate(LocalDate.of(2026, 1, 9));
        createDTO.setReason("Congé annuel");

        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();
        responseDTO.setId(100L);
        responseDTO.setEmployeeId(1L);
        responseDTO.setLeaveTypeId(1L);
        responseDTO.setStartDate(LocalDate.of(2026, 1, 5));
        responseDTO.setEndDate(LocalDate.of(2026, 1, 9));
        responseDTO.setBusinessDays(5);
        responseDTO.setStatus(LeaveStatus.EN_ATTENTE);

        when(leaveRequestService.create(eq(1L), any(LeaveRequestCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/employees/1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.businessDays").value(5))
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"));
    }

    @Test
    void shouldReturn400WhenBusinessRuleViolatedOnCreate() throws Exception {
        LeaveRequestCreateDTO createDTO = new LeaveRequestCreateDTO();
        createDTO.setLeaveTypeId(1L);
        createDTO.setStartDate(LocalDate.of(2026, 1, 5));
        createDTO.setEndDate(LocalDate.of(2026, 1, 9));
        createDTO.setReason("Solde insuffisant");

        when(leaveRequestService.create(eq(1L), any(LeaveRequestCreateDTO.class)))
                .thenThrow(new BusinessRuleException("Solde insuffisant pour 2026 : 2 jour(s) disponible(s)"));

        mockMvc.perform(post("/api/employees/1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Solde insuffisant pour 2026 : 2 jour(s) disponible(s)"));
    }

    @Test
    void shouldReturn404WhenLeaveRequestNotFound() throws Exception {
        when(leaveRequestService.findById(999L)).thenThrow(new ResourceNotFoundException("Demande de congé", 999L));

        mockMvc.perform(get("/api/leave-requests/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Demande de congé avec l'id 999 introuvable"));
    }

    @Test
    void shouldReturn409WhenOverlappingLeaveRequest() throws Exception {
        LeaveRequestCreateDTO createDTO = new LeaveRequestCreateDTO();
        createDTO.setLeaveTypeId(1L);
        createDTO.setStartDate(LocalDate.of(2026, 1, 5));
        createDTO.setEndDate(LocalDate.of(2026, 1, 9));
        createDTO.setReason("Chevauchement");

        when(leaveRequestService.create(eq(1L), any(LeaveRequestCreateDTO.class)))
                .thenThrow(new ConflictException("Cette demande chevauche une demande existante"));

        mockMvc.perform(post("/api/employees/1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cette demande chevauche une demande existante"));
    }

    @Test
    void shouldReturn200WhenApprovingLeaveRequest() throws Exception {
        LeaveRequestResponseDTO responseDTO = new LeaveRequestResponseDTO();
        responseDTO.setId(100L);
        responseDTO.setStatus(LeaveStatus.APPROUVEE);

        when(leaveRequestService.approve(100L)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/leave-requests/100/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROUVEE"));
    }
}

