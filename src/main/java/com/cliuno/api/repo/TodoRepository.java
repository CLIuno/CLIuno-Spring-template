package com.cliuno.api.repo;

import com.cliuno.api.entity.Todo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Todo> findAllByOrderByCreatedAtDesc();
}
