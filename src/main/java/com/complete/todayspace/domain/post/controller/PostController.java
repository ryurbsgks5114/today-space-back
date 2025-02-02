package com.complete.todayspace.domain.post.controller;

import com.complete.todayspace.domain.comment.dto.CommentResponseDto;
import com.complete.todayspace.domain.comment.dto.CreateCommentRequestDto;
import com.complete.todayspace.domain.comment.service.CommentService;
import com.complete.todayspace.domain.like.service.LikeService;
import com.complete.todayspace.domain.post.dto.CreatePostRequestDto;
import com.complete.todayspace.domain.post.dto.EditPostRequestDto;
import com.complete.todayspace.domain.post.dto.MyPostResponseDto;
import com.complete.todayspace.domain.post.dto.PostResponseDto;
import com.complete.todayspace.domain.post.service.PostService;
import com.complete.todayspace.global.dto.DataResponseDto;
import com.complete.todayspace.global.dto.StatusResponseDto;
import com.complete.todayspace.global.entity.SuccessCode;
import com.complete.todayspace.global.exception.CustomException;
import com.complete.todayspace.global.exception.ErrorCode;
import com.complete.todayspace.global.security.UserDetailsImpl;
import com.complete.todayspace.global.valid.PageValidation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final LikeService likeService;
    private final CommentService commentService;

    @PostMapping("/posts")
    public ResponseEntity<StatusResponseDto> createPost(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid CreatePostRequestDto requestDto
    ) {
        postService.createPost(userDetails.getUser(), requestDto);
        StatusResponseDto response = new StatusResponseDto(SuccessCode.POSTS_CREATE);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/posts")
    public ResponseEntity<DataResponseDto<Page<PostResponseDto>>> getPostPage(
            @RequestParam(defaultValue = "1") String page,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String hashtag,
            @RequestParam(required = false) Boolean topLiked
    ) {
        int pageNumber;
        try {
            pageNumber = Integer.parseInt(page);
            if (pageNumber < 1) {
                throw new CustomException(ErrorCode.INVALID_URL_ACCESS);
            }
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.INVALID_URL_ACCESS);
        }

        Sort sort = Sort.by(
                direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(pageNumber - 1, 5, sort);

        Page<PostResponseDto> responseDto;
        if (topLiked != null && topLiked) {
            responseDto = postService.getTopLikedPosts(PageRequest.of(0, 4));
        } else if (hashtag != null && !hashtag.trim().isEmpty()) {
            responseDto = postService.getPostsByHashtag(hashtag, pageable);
        } else {
            responseDto = postService.getPostPage(pageable);
        }

        DataResponseDto<Page<PostResponseDto>> post = new DataResponseDto<>(SuccessCode.POSTS_GET, responseDto);
        return new ResponseEntity<>(post, HttpStatus.OK);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<DataResponseDto<Page<CommentResponseDto>>> getComments(
            @PathVariable @Min(1) Long postId,
            @RequestParam Map<String, String> params
    ) {
        int pageNumber = PageValidation.pageValidationInParams(params) - 1; // 0 기반 인덱스로 변환

        Pageable pageable = PageRequest.of(pageNumber, 5, Sort.by("createdAt").descending());
        Page<CommentResponseDto> comments = commentService.getCommentsByPostId(postId, pageable);
        DataResponseDto<Page<CommentResponseDto>> responseDto = new DataResponseDto<>(SuccessCode.COMMENT_CREATE, comments);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<DataResponseDto<PostResponseDto>> getPost(
            @Min(1) @PathVariable Long postId
    ) {
        PostResponseDto responseDto = postService.getPost(postId);

        DataResponseDto<PostResponseDto> response = new DataResponseDto<>(SuccessCode.POSTS_GET, responseDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<StatusResponseDto> editPost(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long postId,
            @RequestBody @Valid EditPostRequestDto requestDto
    ) {
        postService.editPost(userDetails.getUser().getId(), postId, requestDto);
        StatusResponseDto responseDto = new StatusResponseDto(SuccessCode.POSTS_UPDATE);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<StatusResponseDto> deletePost(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable @Min(1) Long postId
    ) {
        postService.deletePost(userDetails.getUser().getId(), postId);
        StatusResponseDto responseDto = new StatusResponseDto(SuccessCode.POSTS_DELETE);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<DataResponseDto<Map<String, Object>>> getLikes(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable @Min(1) Long postId
    ) {
        boolean isLiked = likeService.checkIfLiked(userDetails.getUser(), postId);
        long likeCount = likeService.countLikes(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", likeCount);

        DataResponseDto<Map<String, Object>> responseDto = new DataResponseDto<>(SuccessCode.LIKES_CREATE, response);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<DataResponseDto<Map<String, Object>>> toggleLike(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable @Min(1) Long postId
    ) {
        boolean isLiked = likeService.toggleLike(userDetails.getUser(), postId);
        long likeCount = likeService.countLikes(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("isLiked", isLiked);
        response.put("likeCount", likeCount);

        DataResponseDto<Map<String, Object>> responseDto = new DataResponseDto<>(SuccessCode.LIKES_CREATE, response);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<StatusResponseDto> addComment(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody CreateCommentRequestDto requestDto
    ) {
        commentService.addComment(userDetails.getUser(), postId, requestDto);
        return new ResponseEntity<>(new StatusResponseDto(SuccessCode.COMMENT_CREATE), HttpStatus.CREATED);
    }

    @GetMapping("/posts/my")
    public ResponseEntity<DataResponseDto<Page<MyPostResponseDto>>> getMyPostList(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam Map<String, String> params
    ) {
        int page = PageValidation.pageValidationInParams(params);

        Page<MyPostResponseDto> responseDto = postService.getMyPostList(userDetails.getUser().getId(), page - 1);
        DataResponseDto<Page<MyPostResponseDto>> dataResponseDto = new DataResponseDto<>(SuccessCode.POSTS_GET, responseDto);

        return new ResponseEntity<>(dataResponseDto, HttpStatus.OK);
    }

}
