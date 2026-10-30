package ma.atlasrh.repository;

import ma.atlasrh.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByManagerId(Long managerId);

    List<Employee> findByManagerId(Long managerId);

    /**
     * Recherche combinable par nom (prénom ou nom de famille) et/ou département.
     * Les paramètres null sont ignorés grâce aux conditions OR IS NULL.
     */
    @Query("SELECT e FROM Employee e WHERE " +
           "(:name IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId)")
    Page<Employee> search(@Param("name") String name,
                          @Param("departmentId") Long departmentId,
                          Pageable pageable);
}
