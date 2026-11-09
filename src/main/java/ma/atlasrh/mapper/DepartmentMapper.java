package ma.atlasrh.mapper;

import ma.atlasrh.dto.DepartmentRequestDTO;
import ma.atlasrh.dto.DepartmentResponseDTO;
import ma.atlasrh.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponseDTO toResponseDTO(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    Department toEntity(DepartmentRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    void updateEntityFromDTO(DepartmentRequestDTO dto, @MappingTarget Department department);
}
