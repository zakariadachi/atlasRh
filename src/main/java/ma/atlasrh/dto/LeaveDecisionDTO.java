package ma.atlasrh.dto;

public class LeaveDecisionDTO {

    private String comment;

    public LeaveDecisionDTO() {}

    public LeaveDecisionDTO(String comment) {
        this.comment = comment;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
