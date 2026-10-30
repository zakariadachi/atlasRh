package ma.atlasrh.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO pour la décision du manager sur une demande de congé.
 * En cas de refus, le commentaire est obligatoire selon les règles du brief.
 */
public class LeaveDecisionDTO {

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    private String comment;

    public LeaveDecisionDTO() {
    }

    public LeaveDecisionDTO(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
