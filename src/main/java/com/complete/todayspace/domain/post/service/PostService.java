package com.complete.todayspace.domain.post.service;

import com.complete.todayspace.domain.common.S3Provider;
import com.complete.todayspace.domain.post.dto.CreatePostRequestDto;
import com.complete.todayspace.domain.post.dto.EditPostRequestDto;
import com.complete.todayspace.domain.post.dto.PostResponseDto;
import com.complete.todayspace.domain.post.entitiy.ImagePost;
import com.complete.todayspace.domain.post.entitiy.Post;
import com.complete.todayspace.domain.post.repository.ImagePostRepository;
import com.complete.todayspace.domain.post.repository.PostRepository;
import com.complete.todayspace.domain.product.repository.ImageProductRepository;
import com.complete.todayspace.domain.product.service.S3Service;
import com.complete.todayspace.domain.user.entity.User;
import com.complete.todayspace.global.exception.CustomException;
import com.complete.todayspace.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ImagePostRepository imagePostRepository;
    private final S3Provider s3Provider;

    @Transactional
    public void createPost(User user, CreatePostRequestDto requestDto,  List<MultipartFile> postImage) {

        List<String> fileUrls = s3Provider.uploadFile("post", postImage);

        Post savePost = new Post(requestDto.getContent(), user);
        postRepository.save(savePost);

        for (String fileUrl : fileUrls) {
            ImagePost imagePost = new ImagePost(fileUrl, savePost);
            imagePostRepository.save(imagePost);
        }

    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPostPage(Pageable pageable) {
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.map(post -> new PostResponseDto(post.getId(), post.getContent(), post.getUpdatedAt()));
    }

    @Transactional
    public void editPost(Long userId, Long postId, EditPostRequestDto requestDto) {
        Post post = postRepository.findByIdAndUserId(postId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.updatePost(requestDto.getContent());
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findByIdAndUserId(postId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND));
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getMyPostList(Long id, int page) {

        int size = 8;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findByUserIdOrderByCreatedAtDesc(id, pageable);

        return postPage.map( (post) -> new PostResponseDto(post.getId(), post.getContent(), post.getCreatedAt()));
    }

    private boolean isPostOwner(Long postId, Long userId) {
        return postRepository.existsByIdAndUserId(postId, userId);
    }
}
