package com.speakmaster.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.common.dto.Result;
import com.speakmaster.community.document.PostDocument;
import com.speakmaster.community.dto.PostDTO;
import com.speakmaster.community.service.IPostFavoriteService;
import com.speakmaster.community.service.IPostLikeService;
import com.speakmaster.community.service.IPostService;
import com.speakmaster.community.service.PostSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class PostController {

    private final IPostService postService;
    private final IPostLikeService likeService;
    private final IPostFavoriteService favoriteService;
    private final PostSearchService searchService;

    /**
     * 发布帖子 (用户�?
     */
    @PostMapping("/user/community/posts")
    public Result<PostDTO> createPost(@RequestBody PostDTO postDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        PostDTO post = postService.createPost(postDTO, userId);
        return Result.success(post);
    }

    /**
     * 获取帖子列表 (用户�?
     */
    @GetMapping("/user/community/posts")
    public Result<Page<PostDTO>> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<PostDTO> posts = postService.getPostList(page, size, userId);
        return Result.success(posts);
    }

    /**
     * 获取帖子详情 (用户�?
     */
    @GetMapping("/user/community/posts/{id}")
    public Result<PostDTO> getPostById(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        PostDTO post = postService.getPostById(id, userId);
        return Result.success(post);
    }

    /**
     * 获取我的帖子 (用户�?
     */
    @GetMapping("/user/community/posts/my")
    public Result<Page<PostDTO>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<PostDTO> posts = postService.getUserPosts(userId, page, size);
        return Result.success(posts);
    }

    /**
     * 获取热门帖子 (用户�?
     */
    @GetMapping("/user/community/posts/hot")
    public Result<Page<PostDTO>> getHotPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<PostDTO> posts = postService.getHotPosts(page, size, userId);
        return Result.success(posts);
    }

    /**
     * 更新帖子 (用户�?
     */
    @PutMapping("/user/community/posts/{id}")
    public Result<PostDTO> updatePost(
            @PathVariable Long id,
            @RequestBody PostDTO postDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        PostDTO post = postService.updatePost(id, postDTO, userId);
        return Result.success(post);
    }

    /**
     * 删除帖子 (用户�?
     */
    @DeleteMapping("/user/community/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        postService.deletePost(id, userId);
        return Result.success();
    }

    /**
     * 点赞帖子 (用户�?
     */
    @PostMapping("/user/community/posts/{id}/like")
    public Result<Void> likePost(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        likeService.likePost(id, userId);
        return Result.success();
    }

    /**
     * 取消点赞 (用户�?
     */
    @DeleteMapping("/user/community/posts/{id}/like")
    public Result<Void> unlikePost(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        likeService.unlikePost(id, userId);
        return Result.success();
    }

    /**
     * 获取点赞�?(用户�?
     */
    @GetMapping("/user/community/posts/{id}/likes/count")
    public Result<Long> getLikeCount(@PathVariable Long id) {
        Long count = likeService.getLikeCount(id);
        return Result.success(count);
    }

    /**
     * 收藏帖子 (用户�?
     */
    @PostMapping("/user/community/posts/{id}/favorite")
    public Result<Void> favoritePost(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        favoriteService.favoritePost(id, userId);
        return Result.success();
    }

    /**
     * 取消收藏 (用户�?
     */
    @DeleteMapping("/user/community/posts/{id}/favorite")
    public Result<Void> unfavoritePost(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        favoriteService.unfavoritePost(id, userId);
        return Result.success();
    }

    /**
     * 搜索帖子 (用户�?- Elasticsearch全文搜索)
     */
    @GetMapping("/user/community/posts/search")
    public Result<List<PostDocument>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PostDocument> results;
        if (category != null && !category.isEmpty()) {
            results = searchService.searchByCategory(keyword, category, page, size);
        } else {
            results = searchService.search(keyword, page, size);
        }
        return Result.success(results);
    }

    /**
     * 全量同步帖子到ES (管理�?
     */
    @PostMapping("/admin/community/posts/sync-es")
    public Result<Void> syncPostsToEs() {
        searchService.syncAllPosts();
        return Result.success();
    }
}
