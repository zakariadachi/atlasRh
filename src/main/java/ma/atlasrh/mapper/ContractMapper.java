package ma.atlasrh.mapper;

import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.dto.ContractUpdateDTO;
import ma.atlasrh.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeFullName", expression = "java(contract.getEmployee() != null ? contract.getEmployee().getFirstName() + \" \" + contract.getEmployee().getLastName() : null)")
    ContractResponseDTO toResponseDTO(Contract contract);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Contract toEntity(ContractCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    void updateEntityFromDTO(ContractUpdateDTO dto, @MappingTarget Contract contract);
}
