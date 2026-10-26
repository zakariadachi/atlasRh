package ma.atlasrh.mapper;

import ma.atlasrh.dto.EmployeeCreateDTO;
import ma.atlasrh.dto.EmployeeResponseDTO;
import ma.atlasrh.dto.EmployeeUpdateDTO;
import ma.atlasrh.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(target = "managerFullName", expression = "java(employee.getManager() != null ? employee.getManager().getFirstName() + \" \" + employee.getManager().getLastName() : null)")
    EmployeeResponseDTO toResponseDTO(Employee employee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "subordinates", ignore = true)
    @Mapping(target = "contracts", ignore = true)
    Employee toEntity(EmployeeCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "matricule", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "subordinates", ignore = true)
    @Mapping(target = "contracts", ignore = true)
    void updateEntityFromDTO(EmployeeUpdateDTO dto, @MappingTarget Employee employee);
}
