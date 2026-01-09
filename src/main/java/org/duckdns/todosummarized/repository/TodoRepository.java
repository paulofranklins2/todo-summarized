package org.duckdns.todosummarized.repository;

import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.repository.projection.PriorityCountProjection;
import org.duckdns.todosummarized.repository.projection.StatusCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Todo entity.
 */
public interface TodoRepository extends JpaRepository<Todo, UUID>, JpaSpecificationExecutor<Todo> {

    /**
     * Find a todo by ID and user.
     *
     * @param id   the todo ID
     * @param user the user who owns the todo
     * @return the todo if found and owned by the user
     */
    Optional<Todo> findByIdAndUser(UUID id, User user);

    /**
     * Deletes the todo with the given id and user.
     *
     * @param id   the id of the todo to be deleted
     * @param user the user who owns the todo
     * @return the number of rows deleted
     */
    long deleteByIdAndUser(UUID id, User user);

    /**
     * Count todos by user.
     *
     * @param user the user
     * @return the count
     */
    long countByUser(User user);

    @Query("select t.status as status, count(t) as count from Todo t where t.user = :user group by t.status")
    List<StatusCountProjection> countGroupedByStatusAndUser(@Param("user") User user);

    @Query("select t.priority as priority, count(t) as count from Todo t where t.user = :user group by t.priority")
    List<PriorityCountProjection> countGroupedByPriorityAndUser(@Param("user") User user);

    @Query("""
            select count(t)
            from Todo t
            where t.user = :user
              and t.dueDate < :now
              and t.status not in :excludedStatuses
            """)
    long countOverdueByUser(
            @Param("user") User user,
            @Param("now") LocalDateTime now,
            @Param("excludedStatuses") Collection<TaskStatus> excludedStatuses
    );

    @Query("""
            select count(t)
            from Todo t
            where t.user = :user
              and t.dueDate >= :start
              and t.dueDate < :end
            """)
    long countDueBetweenByUser(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
