package com.example.botguard.controller;

import com.example.botguard.dto.CreateCommentRequest;
import com.example.botguard.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(commentService.addComment(request));
    }
}
