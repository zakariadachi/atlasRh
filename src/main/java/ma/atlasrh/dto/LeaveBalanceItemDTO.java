package ma.atlasrh.dto;

public class LeaveBalanceItemDTO {

    private Long leaveTypeId;
    private String leaveTypeName;
    private int annualDays;
    private int approvedDays;
    private int remainingBalance;

    public LeaveBalanceItemDTO() {
    }

    public LeaveBalanceItemDTO(Long leaveTypeId, String leaveTypeName, int annualDays, int approvedDays, int remainingBalance) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.annualDays = annualDays;
        this.approvedDays = approvedDays;
        this.remainingBalance = remainingBalance;
    }

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public int getAnnualDays() {
        return annualDays;
    }

    public void setAnnualDays(int annualDays) {
        this.annualDays = annualDays;
    }

    public int getApprovedDays() {
        return approvedDays;
    }

    public void setApprovedDays(int approvedDays) {
        this.approvedDays = approvedDays;
    }

    public int getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(int remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}
