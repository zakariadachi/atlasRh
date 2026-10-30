package ma.atlasrh.mapper;

import ma.atlasrh.dto.LeaveRequestResponseDTO;
import ma.atlasrh.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeFullName", expression = "java(leaveRequest.getEmployee() != null ? leaveRequest.getEmployee().getFirstName() + \" \" + leaveRequest.getEmployee().getLastName() : null)")
    @Mapping(source = "leaveType.id", target = "leaveTypeId")
    @Mapping(source = "leaveType.name", target = "leaveTypeName")
    LeaveRequestResponseDTO toResponseDTO(LeaveRequest leaveRequest);
}
