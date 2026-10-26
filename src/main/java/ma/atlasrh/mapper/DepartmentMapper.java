package ma.atlasrh.mapper;

import ma.atlasrh.dto.DepartmentRequestDTO;
import ma.atlasrh.dto.DepartmentResponseDTO;
import ma.atlasrh.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    DepartmentResponseDTO toResponseDTO(Department department);

    Department toEntity(DepartmentRequestDTO dto);

    void updateEntityFromDTO(DepartmentRequestDTO dto, @MappingTarget Department department);
}
