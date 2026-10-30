package ma.atlasrh.dto;

import java.util.List;

public class EmployeeLeaveBalanceDTO {

    private Long employeeId;
    private String employeeFullName;
    private int year;
    private List<LeaveBalanceItemDTO> balances;

    public EmployeeLeaveBalanceDTO() {
    }

    public EmployeeLeaveBalanceDTO(Long employeeId, String employeeFullName, int year, List<LeaveBalanceItemDTO> balances) {
        this.employeeId = employeeId;
        this.employeeFullName = employeeFullName;
        this.year = year;
        this.balances = balances;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeFullName() {
        return employeeFullName;
    }

    public void setEmployeeFullName(String employeeFullName) {
        this.employeeFullName = employeeFullName;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<LeaveBalanceItemDTO> getBalances() {
        return balances;
    }

    public void setBalances(List<LeaveBalanceItemDTO> balances) {
        this.balances = balances;
    }
}
