package org.duckdns.todosummarized.repository;

import org.duckdns.todosummarized.domains.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for TODO entity - handles all database operations.
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, UUID>, JpaSpecificationExecutor<Todo> {

    @Modifying
    @Query("DELETE FROM Todo t WHERE t.id = :id")
    int deleteByIdReturningCount(@Param("id") UUID id);
}

