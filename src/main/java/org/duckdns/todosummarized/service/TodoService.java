package org.duckdns.todosummarized.service;

import lombok.RequiredArgsConstructor;
import org.duckdns.todosummarized.domains.entity.Todo;
import org.duckdns.todosummarized.domains.entity.User;
import org.duckdns.todosummarized.domains.enums.TaskStatus;
import org.duckdns.todosummarized.dto.TodoMapper;
import org.duckdns.todosummarized.repository.TodoQuery;
import org.duckdns.todosummarized.dto.TodoRequestDTO;
import org.duckdns.todosummarized.exception.TodoNotFoundException;
import org.duckdns.todosummarized.repository.TodoRepository;
import org.duckdns.todosummarized.repository.spec.TodoSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Service layer for Todo operations.
 * Contains all the business logic for managing todos.
 * All operations are scoped to the authenticated user.
 */
@Service
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;
    private final Clock clock;

    /**
     * Create a new todo for the specified user.
     *
     * @param todo the todo request DTO
     * @param user the authenticated user
     * @return the created todo
     */
    public Todo createTodo(TodoRequestDTO todo, User user) {
        return todoRepository.save(TodoMapper.toNewEntity(todo, user));
    }

    /**
     * Get a todo by ID for the specified user.
     *
     * @param id   the todo ID
     * @param user the authenticated user
     * @return the todo
     * @throws TodoNotFoundException if not found or not owned by user
     */
    @Transactional(readOnly = true)
    public Todo getTodoById(UUID id, User user) {
        return todoRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    /**
     * Update an existing todo for the specified user.
     *
     * @param id          the todo ID
     * @param updatedTodo the updated todo data
     * @param user        the authenticated user
     * @return the updated todo
     * @throws TodoNotFoundException if not found or not owned by user
     */
    @Transactional
    public Todo updateTodo(UUID id, TodoRequestDTO updatedTodo, User user) {
        Todo existingTodo = getTodoById(id, user);
        TodoMapper.patchEntity(updatedTodo, existingTodo);
        return existingTodo;
    }

    /**
     * Delete a todo by its ID for the specified user.
     *
     * @param id   the todo ID
     * @param user the authenticated user
     * @throws TodoNotFoundException if not found or not owned by user
     */
    @Transactional
    public void deleteTodo(UUID id, User user) {
        long deleted = todoRepository.deleteByIdAndUser(id, user);

        if (deleted == 0) {
            throw new TodoNotFoundException(id);
        }
    }

    /**
     * Update the status of a todo for the specified user.
     *
     * @param id     the todo ID
     * @param status the new status
     * @param user   the authenticated user
     * @return the updated todo
     * @throws TodoNotFoundException if not found or not owned by user
     */
    @Transactional
    public Todo updateStatus(UUID id, TaskStatus status, User user) {
        Todo todo = getTodoById(id, user);
        todo.setStatus(status);
        return todo;
    }

    /**
     * Search for todos based on the given query, scoped to the specified user.
     *
     * @param query    the search query
     * @param pageable the pagination settings
     * @param user     the authenticated user
     * @return a page of todos belonging to the user
     */
    @Transactional(readOnly = true)
    public Page<Todo> search(TodoQuery query, Pageable pageable, User user) {
        return todoRepository.findAll(
                TodoSpecs.byQueryAndUser(query, clock, user),
                pageable
        );
    }
}