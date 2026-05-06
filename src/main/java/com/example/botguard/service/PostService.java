package com.example.botguard.service;

import com.example.botguard.domain.Post;
import com.example.botguard.domain.User;
import com.example.botguard.dto.CreatePostRequest;
import com.example.botguard.repository.PostRepository;
import com.example.botguard.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public PostService(PostRepository postRepository, UserRepository userRepository, RedisTemplate<String, Object> redisTemplate) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Post createPost(CreatePostRequest request) {
        User author = userRepository.findById(request.authorId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content());
        return postRepository.save(post);
    }
    
    @Transactional
    public void likePost(Long postId, Long userId) {
        postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));
            
        // Phase 2: Instantly increment Redis keys based on interactions (Human Like = 20)
        String viralityKey = "virality:post:" + postId;
        redisTemplate.opsForValue().increment(viralityKey, 20);
    }
}
