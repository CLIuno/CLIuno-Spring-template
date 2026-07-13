package com.cliuno.api.controller;

import com.cliuno.api.entity.Comment;
import com.cliuno.api.entity.Post;
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
@RequestMapping("/api/v1/posts")
public class PostController {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final AuthSupport auth;

    public PostController(
            PostRepository posts, CommentRepository comments, AuthSupport auth) {
        this.posts = posts;
        this.comments = comments;
        this.auth = auth;
    }

    private Post find(Long id) {
        return posts.findById(id).orElseThrow(() -> new ApiException(404, "Post not found"));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> index(HttpServletRequest request) {
        auth.require(request);
        return Api.ok("Posts", Api.data("posts", posts.findAllByOrderByCreatedAtDesc()));
    }

    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> currentUser(HttpServletRequest request) {
        User user = auth.require(request);
        return Api.ok("Posts", Api.data("posts", posts.findByUserIdOrderByCreatedAtDesc(user.id)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> store(
            HttpServletRequest request, @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        String title = body.get("title");
        String content = body.get("content");
        if (title == null || content == null) {
            throw new ApiException(400, "Title and Content are required");
        }

        Post post = new Post();
        post.title = title;
        post.content = content;
        post.user = user;
        posts.save(post);
        return Api.created("Post created successfully", Api.data("post", post));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> show(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        return Api.ok("Post", Api.data("post", find(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        auth.require(request);
        Post post = find(id);
        if (body.get("title") != null) {
            post.title = body.get("title");
        }
        if (body.get("content") != null) {
            post.content = body.get("content");
        }
        if (body.get("imageUrl") != null) {
            post.imageUrl = body.get("imageUrl");
        }
        posts.save(post);
        return Api.ok("Post updated", Api.data("post", post));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> destroy(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        posts.delete(find(id));
        return Api.ok("Post deleted successfully", null);
    }

    @GetMapping("/{id}/user")
    public ResponseEntity<Map<String, Object>> author(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        return Api.ok("User found", Api.data("user", find(id).user));
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request, @PathVariable Long postId) {
        auth.require(request);
        return Api.ok(
                "Comments", Api.data("comments", comments.findByPostIdOrderByCreatedAtAsc(postId)));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> storeComment(
            HttpServletRequest request,
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        User user = auth.require(request);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new ApiException(400, "Content is required");
        }

        Comment comment = new Comment();
        comment.content = content;
        comment.user = user;
        comment.post = find(postId);
        comments.save(comment);
        return Api.created("Comment created successfully", Api.data("comment", comment));
    }

    @PatchMapping("/{postId}/comments/{id}")
    public ResponseEntity<Map<String, Object>> updateComment(
            HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        auth.require(request);
        Comment comment = comments.findById(id)
                .orElseThrow(() -> new ApiException(404, "Comment not found"));
        if (body.get("content") != null) {
            comment.content = body.get("content");
        }
        comments.save(comment);
        return Api.ok("Comment updated", Api.data("comment", comment));
    }

    @DeleteMapping("/{postId}/comments/{id}")
    public ResponseEntity<Map<String, Object>> destroyComment(
            HttpServletRequest request, @PathVariable Long id) {
        auth.require(request);
        Comment comment = comments.findById(id)
                .orElseThrow(() -> new ApiException(404, "Comment not found"));
        comments.delete(comment);
        return Api.ok("Comment deleted successfully", null);
    }
}
