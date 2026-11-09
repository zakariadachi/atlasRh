package ma.atlasrh.dto;

public class LeaveBalanceItemDTO {

    private Long leaveTypeId;
    private String leaveTypeName;
    private Integer annualDays;
    private Integer approvedDays;
    private Integer remainingBalance;

    public LeaveBalanceItemDTO() {}

    public LeaveBalanceItemDTO(Long leaveTypeId, String leaveTypeName,
                               Integer annualDays, Integer approvedDays, Integer remainingBalance) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.annualDays = annualDays;
        this.approvedDays = approvedDays;
        this.remainingBalance = remainingBalance;
    }

    public Long getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Long leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public Integer getAnnualDays() { return annualDays; }
    public void setAnnualDays(Integer annualDays) { this.annualDays = annualDays; }
    public Integer getApprovedDays() { return approvedDays; }
    public void setApprovedDays(Integer approvedDays) { this.approvedDays = approvedDays; }
    public Integer getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(Integer remainingBalance) { this.remainingBalance = remainingBalance; }
}
