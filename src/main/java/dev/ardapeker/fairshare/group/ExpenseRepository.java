package dev.ardapeker.fairshare.group;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data writes the SQL for these from the method names. */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByCreatedAtAscIdAsc(Long groupId);

    Optional<Expense> findByIdAndGroupId(Long id, Long groupId);
}
