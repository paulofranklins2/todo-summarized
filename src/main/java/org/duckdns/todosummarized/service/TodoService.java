package org.duckdns.todosummarized.service;

import lombok.RequiredArgsConstructor;
import org.duckdns.todosummarized.domains.entity.Todo;
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
 */
@Service
@RequiredArgsConstructor
public class TodoService {
    private final TodoRepository todoRepository;
    private final Clock clock;

    /**
     * Create a new todo.
     */
    public Todo createTodo(TodoRequestDTO todo) {
        return todoRepository.save(TodoMapper.toNewEntity(todo));
    }

    /**
     * Get a todo by ID.
     *
     * @throws TodoNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public Todo getTodoById(UUID id) {
        return todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
    }

    /**
     * Update an existing todo.
     *
     * @throws TodoNotFoundException if not found
     */
    @Transactional
    public Todo updateTodo(UUID id, TodoRequestDTO updatedTodo) {
        Todo existingTodo = getTodoById(id);
        TodoMapper.patchEntity(updatedTodo, existingTodo);
        return existingTodo;
    }

    /**
     * Delete a todo by its ID.
     *
     * @throws TodoNotFoundException if not found
     */
    @Transactional
    public void deleteTodo(UUID id) {
        long deleted = todoRepository.deleteTodoById(id);

        if (deleted == 0) {
            throw new TodoNotFoundException(id);
        }
    }


    /**
     * Update the status of a todo.
     *
     * @throws TodoNotFoundException if not found
     */
    @Transactional
    public Todo updateStatus(UUID id, TaskStatus status) {
        Todo todo = getTodoById(id);
        todo.setStatus(status);
        return todo;
    }

    /**
     * Search for todos based on the given query.
     */
    @Transactional(readOnly = true)
    public Page<Todo> search(TodoQuery query, Pageable pageable) {
        return todoRepository.findAll(
                TodoSpecs.byQuery(query, clock),
                pageable
        );
    }
}