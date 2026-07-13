package com.cliuno.api.controller;

import com.cliuno.api.entity.Todo;
import com.cliuno.api.entity.User;
import com.cliuno.api.repo.*;
import com.cliuno.api.support.Api;
import com.cliuno.api.support.ApiException;
import com.cliuno.api.support.AuthSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {
    private final TodoRepository todos;
    private final AuthSupport auth;

    public TodoController(TodoRepository todos, AuthSupport auth) {
        this.todos = todos;
        this.auth = auth;
    }

    private Todo find(Long id) {
        return todos.findById(id).orElseThrow(() -> new ApiException(404, "Todo not found"));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> index(HttpServletRequest request) {
        auth.require(request);
        return Api.ok("Todos", Api.data("todos", todos.findAllByOrderByCreatedAtDesc()));
    }

    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> currentUser(HttpServletRequest request) {
        User user = auth.require(request);
        return Api.ok("Todos", Api.data("todos", todos.findByUserIdOrderByCreatedAtDesc(user.id)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> store(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            throw new ApiException(400, "Title is required");
        }

        Todo todo = new Todo();
        todo.title = title;
        todo.description = body.getOrDefault("description", "");
        todo.user = user;
        todos.save(todo);
        return Api.created("Todo created successfully", Api.data("todo", todo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> show(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        return Api.ok("Todo", Api.data("todo", find(id)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        Todo todo = find(id);
        todo.isCompleted = !todo.isCompleted;
        todos.save(todo);
        return Api.ok("Todo toggled", Api.data("todo", todo));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        auth.require(request);
        Todo todo = find(id);
        if (body.get("title") instanceof String title) {
            todo.title = title;
        }
        if (body.get("description") instanceof String description) {
            todo.description = description;
        }
        if (body.get("is_completed") instanceof Boolean completed) {
            todo.isCompleted = completed;
        }
        todos.save(todo);
        return Api.ok("Todo updated", Api.data("todo", todo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> destroy(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        todos.delete(find(id));
        return Api.ok("Todo deleted successfully", null);
    }
}
