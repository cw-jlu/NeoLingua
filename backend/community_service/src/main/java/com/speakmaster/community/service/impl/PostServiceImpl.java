package com.speakmaster.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import com.speakmaster.community.dto.PostDTO;
import com.speakmaster.community.entity.Post;
import com.speakmaster.community.entity.PostFavorite;
import com.speakmaster.community.entity.PostLike;
import com.speakmaster.community.mapper.PostFavoriteMapper;
import com.speakmaster.community.mapper.PostLikeMapper;
import com.speakmaster.community.mapper.PostMapper;
import com.speakmaster.community.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {

    private final PostMapper postMapper;
    private final PostLikeMapper likeMapper;
    private final PostFavoriteMapper favoriteMapper;
    private final RedisUtil redisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostDTO createPost(PostDTO postDTO, Long userId) {
        Post post = new Post();
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        post.setAuthorId(userId);
        post.setCategory(postDTO.getCategory());
        post.setTags(postDTO.getTags());
        post.setCoverImage(postDTO.getCoverImage());
        post.setStatus(1);

        postMapper.insert(post);
        log.info("发布帖子成功: postId={}, authorId={}", post.getId(), userId);
        return convertToDTO(post, userId);
    }

    @Override
    public Page<PostDTO> getPostList(int page, int size, Long userId) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0)
                .orderByDesc(Post::getCreateTime);
        
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        Page<PostDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PostDTO> dtoList = result.getRecords().stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    @Cacheable(value = "post", key = "#postId")
    public PostDTO getPostById(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 增加浏览量
        incrementViewCount(postId);

        return convertToDTO(post, userId);
    }

    @Override
    public Page<PostDTO> getUserPosts(Long userId, int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getAuthorId, userId)
                .eq(Post::getDeleted, 0)
                .orderByDesc(Post::getCreateTime);
        
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        Page<PostDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PostDTO> dtoList = result.getRecords().stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public Page<PostDTO> getHotPosts(int page, int size, Long userId) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0)
                .orderByDesc(Post::getLikeCount)
                .orderByDesc(Post::getCreateTime);
        
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        Page<PostDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PostDTO> dtoList = result.getRecords().stream()
                .map(post -> convertToDTO(post, userId))
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public PostDTO updatePost(Long postId, PostDTO postDTO, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 检查权限
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (postDTO.getTitle() != null) post.setTitle(postDTO.getTitle());
        if (postDTO.getContent() != null) post.setContent(postDTO.getContent());
        if (postDTO.getCategory() != null) post.setCategory(postDTO.getCategory());
        if (postDTO.getTags() != null) post.setTags(postDTO.getTags());
        if (postDTO.getCoverImage() != null) post.setCoverImage(postDTO.getCoverImage());

        postMapper.updateById(post);
        log.info("更新帖子成功: postId={}", postId);
        return convertToDTO(post, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public void deletePost(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 检查权限
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        post.markDeleted();
        postMapper.updateById(post);
        log.info("删除帖子成功: postId={}", postId);
    }

    @Override
    public Page<PostDTO> adminGetPostList(Integer status, int page, int size) {
        Page<Post> postPage = new Page<>(page, size);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Post::getStatus, status);
        }
        wrapper.eq(Post::getDeleted, 0)
                .orderByDesc(Post::getCreateTime);
        
        Page<Post> result = postMapper.selectPage(postPage, wrapper);
        Page<PostDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PostDTO> dtoList = result.getRecords().stream()
                .map(post -> convertToDTO(post, null))
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public PostDTO adminGetPostById(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return convertToDTO(post, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public void adminDeletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        post.markDeleted();
        postMapper.updateById(post);
        log.info("[管理端] 删除帖子: postId={}", postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public PostDTO adminPinPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        post.setIsPinned(1);
        postMapper.updateById(post);
        log.info("[管理端] 置顶帖子: postId={}", postId);
        return convertToDTO(post, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public PostDTO adminUnpinPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        post.setIsPinned(0);
        postMapper.updateById(post);
        log.info("[管理端] 取消置顶: postId={}", postId);
        return convertToDTO(post, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public PostDTO adminHidePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        post.setIsHidden(1);
        postMapper.updateById(post);
        log.info("[管理端] 隐藏帖子: postId={}", postId);
        return convertToDTO(post, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post", key = "#postId")
    public PostDTO adminShowPost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        post.setIsHidden(0);
        postMapper.updateById(post);
        log.info("[管理端] 显示帖子: postId={}", postId);
        return convertToDTO(post, null);
    }

    @Override
    public Map<String, Object> adminGetPostStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总数
        stats.put("totalPosts", postMapper.selectCount(null));
        
        // 已发布
        LambdaQueryWrapper<Post> publishedWrapper = new LambdaQueryWrapper<>();
        publishedWrapper.eq(Post::getStatus, 1).eq(Post::getDeleted, 0);
        stats.put("publishedPosts", postMapper.selectCount(publishedWrapper));
        
        // 置顶
        LambdaQueryWrapper<Post> pinnedWrapper = new LambdaQueryWrapper<>();
        pinnedWrapper.eq(Post::getIsPinned, 1)
                .eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0);
        stats.put("pinnedPosts", postMapper.selectCount(pinnedWrapper));
        
        return stats;
    }

    /**
     * 增加浏览量(使用Redis HyperLogLog去重)
     */
    private void incrementViewCount(Long postId) {
        String key = "post:view:" + postId;
        // 使用HyperLogLog统计独立访客
        redisUtil.pfAdd(key, String.valueOf(System.currentTimeMillis()));
        
        // 异步更新数据库
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postMapper.updateById(post);
        }
    }

    /**
     * 转换为DTO
     */
    private PostDTO convertToDTO(Post post, Long userId) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setAuthorId(post.getAuthorId());
        dto.setCategory(post.getCategory());
        dto.setTags(post.getTags());
        dto.setCoverImage(post.getCoverImage());
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setViewCount(post.getViewCount());
        dto.setFavoriteCount(post.getFavoriteCount());
        dto.setIsPinned(post.getIsPinned());
        dto.setIsHidden(post.getIsHidden());
        dto.setStatus(post.getStatus());
        dto.setCreateTime(post.getCreateTime() != null ? post.getCreateTime().toString() : null);

        // 检查是否已点赞
        if (userId != null) {
            LambdaQueryWrapper<PostLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(PostLike::getPostId, post.getId())
                    .eq(PostLike::getUserId, userId)
                    .eq(PostLike::getDeleted, 0);
            dto.setIsLiked(likeMapper.selectCount(likeWrapper) > 0);
            
            LambdaQueryWrapper<PostFavorite> favoriteWrapper = new LambdaQueryWrapper<>();
            favoriteWrapper.eq(PostFavorite::getPostId, post.getId())
                    .eq(PostFavorite::getUserId, userId)
                    .eq(PostFavorite::getDeleted, 0);
            dto.setIsFavorited(favoriteMapper.selectCount(favoriteWrapper) > 0);
        }

        return dto;
    }
}
