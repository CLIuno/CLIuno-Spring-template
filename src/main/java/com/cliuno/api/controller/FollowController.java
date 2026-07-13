package com.cliuno.api.controller;

import com.cliuno.api.entity.Follow;
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
@RequestMapping("/api/v1/follows")
public class FollowController {
    private final FollowRepository follows;
    private final UserRepository users;
    private final AuthSupport auth;

    public FollowController(
            FollowRepository follows, UserRepository users, AuthSupport auth) {
        this.follows = follows;
        this.users = users;
        this.auth = auth;
    }

    private User target(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(404, "User not found"));
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<Map<String, Object>> follow(
            HttpServletRequest request, @PathVariable Long userId) {
        User me = auth.require(request);
        User other = target(userId);
        if (me.id.equals(other.id)) {
            throw new ApiException(400, "Cannot follow this user");
        }
        if (follows.findByFollowerIdAndFollowingId(me.id, other.id).isEmpty()) {
            Follow follow = new Follow();
            follow.follower = me;
            follow.following = other;
            follows.save(follow);
        }
        return Api.created("Followed successfully", null);
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Map<String, Object>> unfollow(
            HttpServletRequest request, @PathVariable Long userId) {
        User me = auth.require(request);
        target(userId);
        follows.findByFollowerIdAndFollowingId(me.id, userId).ifPresent(follows::delete);
        return Api.ok("Unfollowed successfully", null);
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<Map<String, Object>> followers(
            HttpServletRequest request, @PathVariable Long userId) {
        auth.require(request);
        target(userId);
        return Api.ok(
                "Followers",
                Api.data(
                        "followers",
                        follows.findByFollowingId(userId).stream().map(f -> f.follower).toList()));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<Map<String, Object>> following(
            HttpServletRequest request, @PathVariable Long userId) {
        auth.require(request);
        target(userId);
        return Api.ok(
                "Following",
                Api.data(
                        "following",
                        follows.findByFollowerId(userId).stream().map(f -> f.following).toList()));
    }

    @GetMapping("/{userId}/is-following")
    public ResponseEntity<Map<String, Object>> isFollowing(
            HttpServletRequest request, @PathVariable Long userId) {
        User me = auth.require(request);
        target(userId);
        boolean following = follows.findByFollowerIdAndFollowingId(me.id, userId).isPresent();
        return Api.ok("Follow status", Api.data("isFollowing", following));
    }
}
