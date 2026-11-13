package ma.atlasrh.repository;

import ma.atlasrh.entity.Department;
import ma.atlasrh.entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department itDepartment;
    private Department hrDepartment;
    private Employee emp2;
    private Employee emp3;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        itDepartment = departmentRepository.save(new Department("IT", "Informatique"));
        hrDepartment = departmentRepository.save(new Department("RH", "Ressources Humaines"));

        Employee emp1 = new Employee("ATL-001", "Amine", "Benali", "amine@atlasrh.ma", "Dev Backend", LocalDate.of(2023, 1, 10));
        emp1.setDepartment(itDepartment);
        emp1 = employeeRepository.save(emp1);

        emp2 = new Employee("ATL-002", "Sara", "Alami", "sara@atlasrh.ma", "Dev Frontend", LocalDate.of(2024, 3, 15));
        emp2.setDepartment(itDepartment);
        emp2.setManager(emp1);
        emp2 = employeeRepository.save(emp2);

        emp3 = new Employee("ATL-003", "Karim", "Benali", "karim@atlasrh.ma", "Chargé RH", LocalDate.of(2022, 5, 20));
        emp3.setDepartment(hrDepartment);
        emp3 = employeeRepository.save(emp3);
    }

    @Test
    @DisplayName("Recherche par nom (insensible à la casse sur prénom ou nom)")
    void shouldSearchEmployeesByName() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Employee> result = employeeRepository.search("benali", null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Employee::getMatricule)
                .containsExactlyInAnyOrder("ATL-001", "ATL-003");
    }

    @Test
    @DisplayName("Recherche filtrée par département")
    void shouldFilterEmployeesByDepartment() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Employee> result = employeeRepository.search(null, itDepartment.getId(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Employee::getMatricule)
                .containsExactlyInAnyOrder("ATL-001", "ATL-002");
    }

    @Test
    @DisplayName("Recherche combinée par nom et par département")
    void shouldSearchEmployeesByNameAndDepartmentCombined() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Employee> result = employeeRepository.search("benali", itDepartment.getId(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getMatricule()).isEqualTo("ATL-001");
    }

    @Test
    @DisplayName("Pagination des résultats")
    void shouldPaginateEmployeeSearchResults() {
        Pageable firstPage = PageRequest.of(0, 2, Sort.by("matricule").ascending());
        Pageable secondPage = PageRequest.of(1, 2, Sort.by("matricule").ascending());

        Page<Employee> page1 = employeeRepository.search(null, null, firstPage);
        Page<Employee> page2 = employeeRepository.search(null, null, secondPage);

        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getContent()).extracting(Employee::getMatricule).containsExactly("ATL-001", "ATL-002");

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent()).extracting(Employee::getMatricule).containsExactly("ATL-003");
    }


    @Test
    @DisplayName("Tri des résultats par nom de famille croissant et décroissant")
    void shouldSortEmployeeSearchResults() {
        Pageable sortAsc = PageRequest.of(0, 10, Sort.by("lastName").ascending());
        Pageable sortDesc = PageRequest.of(0, 10, Sort.by("lastName").descending());

        // Tri croissant
        Page<Employee> ascResult = employeeRepository.search(null, null, sortAsc);

        assertThat(ascResult.getContent())
                .extracting(Employee::getLastName)
                .containsExactly("Alami", "Benali", "Benali");

        // Tri décroissant
        Page<Employee> descResult = employeeRepository.search(null, null, sortDesc);

        assertThat(descResult.getContent())
                .extracting(Employee::getLastName)
                .containsExactly("Benali", "Benali", "Alami");
    }

}
