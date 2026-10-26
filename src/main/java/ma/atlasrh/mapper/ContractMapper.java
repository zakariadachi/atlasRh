package ma.atlasrh.mapper;

import ma.atlasrh.dto.ContractCreateDTO;
import ma.atlasrh.dto.ContractResponseDTO;
import ma.atlasrh.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeFullName", expression = "java(contract.getEmployee() != null ? contract.getEmployee().getFirstName() + \" \" + contract.getEmployee().getLastName() : null)")
    ContractResponseDTO toResponseDTO(Contract contract);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Contract toEntity(ContractCreateDTO dto);
}
