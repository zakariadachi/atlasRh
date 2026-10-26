package ma.atlasrh.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ma.atlasrh.entity.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour la création d'un contrat.
 */
public class ContractCreateDTO {

    @NotNull(message = "Le type de contrat est obligatoire")
    private ContractType type;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Le salaire brut est obligatoire")
    @Positive(message = "Le salaire brut doit être strictement positif")
    private BigDecimal grossSalary;

    public ContractCreateDTO() {
    }

    public ContractType getType() {
        return type;
    }

    public void setType(ContractType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(BigDecimal grossSalary) {
        this.grossSalary = grossSalary;
    }
}
