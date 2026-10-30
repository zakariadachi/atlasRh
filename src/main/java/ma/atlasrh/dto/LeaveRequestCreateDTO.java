package ma.atlasrh.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO pour le dépôt d'une demande de congé par un employé.
 */
public class LeaveRequestCreateDTO {

    @NotNull(message = "Le type de congé est obligatoire")
    private Long leaveTypeId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String reason;

    public LeaveRequestCreateDTO() {
    }

    public LeaveRequestCreateDTO(Long leaveTypeId, LocalDate startDate, LocalDate endDate, String reason) {
        this.leaveTypeId = leaveTypeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
