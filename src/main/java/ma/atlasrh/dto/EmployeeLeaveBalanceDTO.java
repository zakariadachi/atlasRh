package ma.atlasrh.dto;

import java.util.List;

public class EmployeeLeaveBalanceDTO {

    private Long employeeId;
    private String employeeFullName;
    private int year;
    private String seniority;
    private List<LeaveBalanceItemDTO> balances;

    public EmployeeLeaveBalanceDTO() {}

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeFullName() { return employeeFullName; }
    public void setEmployeeFullName(String employeeFullName) { this.employeeFullName = employeeFullName; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }
    public List<LeaveBalanceItemDTO> getBalances() { return balances; }
    public void setBalances(List<LeaveBalanceItemDTO> balances) { this.balances = balances; }
}
