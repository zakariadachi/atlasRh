package ma.atlasrh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Objects;

@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du type de congé est obligatoire")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Le nombre de jours par an est obligatoire")
    @Positive(message = "Le nombre de jours par an doit être strictement positif")
    @Column(name = "days_per_year", nullable = false)
    private Integer daysPerYear;

    public LeaveType() {
    }

    public LeaveType(String name, String description, Integer daysPerYear) {
        this.name = name;
        this.description = description;
        this.daysPerYear = daysPerYear;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDaysPerYear() {
        return daysPerYear;
    }

    public void setDaysPerYear(Integer daysPerYear) {
        this.daysPerYear = daysPerYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveType leaveType = (LeaveType) o;
        return id != null && Objects.equals(id, leaveType.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LeaveType{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", daysPerYear=" + daysPerYear +
                '}';
    }
}
