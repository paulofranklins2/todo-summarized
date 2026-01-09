package org.duckdns.todosummarized.repository;

import org.duckdns.todosummarized.domains.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository for Todo entity.
 */
public interface TodoRepository extends JpaRepository<Todo, UUID>, JpaSpecificationExecutor<Todo> {

    /**
     * Deletes the todo with the given id.
     *
     * @param id the id of the todo to be deleted
     * @return the number of rows deleted
     */
    long deleteTodoById(UUID id);
}
