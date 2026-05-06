package com.example.botguard.service;

import com.example.botguard.domain.Comment;
import com.example.botguard.domain.Post;
import com.example.botguard.dto.CreateCommentRequest;
import com.example.botguard.repository.CommentRepository;
import com.example.botguard.repository.PostRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> horizontalCapScript;

    public CommentService(CommentRepository commentRepository, 
                          PostRepository postRepository,
                          RedisTemplate<String, Object> redisTemplate,
                          RedisScript<Long> horizontalCapScript) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.redisTemplate = redisTemplate;
        this.horizontalCapScript = horizontalCapScript;
    }

    @Transactional
    public Comment addComment(CreateCommentRequest request) {
        Post post = postRepository.findById(request.postId())
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthorId(request.authorId());
        comment.setAuthorType(request.authorType());
        comment.setContent(request.content());
        
        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            
            int newDepth = parent.getDepthLevel() + 1;
            if (newDepth > 20) {
                throw new IllegalStateException("Maximum comment thread depth of 20 reached.");
            }
            
            comment.setParentComment(parent);
            comment.setDepthLevel(newDepth);
        } else {
            comment.setDepthLevel(1);
        }
        
        // --- PHASE 2: REDIS GUARDRAILS ---
        if (request.authorType() == Comment.AuthorType.BOT) {
            
            // 1. Cooldown Cap: SET NX EX
            Long targetHumanId = getTargetHumanId(post, parent);
            if (targetHumanId != null) {
                String cooldownKey = "cooldown:bot:" + request.authorId() + ":human:" + targetHumanId;
                Boolean allowed = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(10));
                if (Boolean.FALSE.equals(allowed)) {
                    throw new IllegalStateException("Bot is on cooldown for this human.");
                }
            }

            // 2. Horizontal Cap: Atomic Lua Script
            String capKey = "post:" + post.getId() + ":bot_replies";
            Long result = redisTemplate.execute(horizontalCapScript, List.of(capKey), 100);
            if (result == null || result == 0) {
                throw new IllegalStateException("Bot reply cap of 100 reached for this post.");
            }
        }
        
        // Commit to PostgreSQL
        Comment saved = commentRepository.save(comment);

        // 3. Virality Score (Instant updates)
        String viralityKey = "virality:post:" + post.getId();
        if (request.authorType() == Comment.AuthorType.BOT) {
            redisTemplate.opsForValue().increment(viralityKey, 1);
        } else {
            redisTemplate.opsForValue().increment(viralityKey, 50);
        }
        
        return saved;
    }

    private Long getTargetHumanId(Post post, Comment parentComment) {
        if (parentComment != null) {
            if (parentComment.getAuthorType() == Comment.AuthorType.USER) {
                return parentComment.getAuthorId();
            }
            return null; // Replying to another bot
        } else {
            // Replying directly to the post, author is a User
            return post.getAuthor().getId();
        }
    }
}
