package com.example.botguard.dto;

import com.example.botguard.domain.Comment.AuthorType;

public record CreateCommentRequest(
    Long postId, 
    Long parentCommentId, 
    Long authorId, 
    AuthorType authorType, 
    String content
) {}
