package ma.atlasrh.mapper;

import ma.atlasrh.dto.LeaveRequestResponseDTO;
import ma.atlasrh.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LeaveRequestMapper {

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeFullName", expression = "java(lr.getEmployee().getFirstName() + \" \" + lr.getEmployee().getLastName())")
    @Mapping(target = "leaveTypeId", source = "leaveType.id")
    @Mapping(target = "leaveTypeName", source = "leaveType.name")
    LeaveRequestResponseDTO toResponseDTO(LeaveRequest lr);
}
