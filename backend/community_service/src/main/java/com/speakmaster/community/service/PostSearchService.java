package com.speakmaster.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.community.document.PostDocument;
import com.speakmaster.community.entity.Post;
import com.speakmaster.community.mapper.PostMapper;
import com.speakmaster.community.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子搜索服务
 * 基于Elasticsearch实现全文搜索
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final PostSearchRepository searchRepository;
    private final PostMapper postMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 全文搜索帖子（标题、内容）
     * 
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    public List<PostDocument> search(String keyword, int page, int size) {
        // 构建多字段搜索条件：标题或内容包含关键词，且状态为已发布
        Criteria criteria = new Criteria("title").matches(keyword)
                .or(new Criteria("content").matches(keyword));
        criteria = criteria.and(new Criteria("status").is(1));

        Query query = new CriteriaQuery(criteria).setPageable(org.springframework.data.domain.PageRequest.of(page, size));
        SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * 按分类搜索
     */
    public List<PostDocument> searchByCategory(String keyword, String category, int page, int size) {
        Criteria criteria = new Criteria("category").is(category)
                .and(new Criteria("title").matches(keyword)
                        .or(new Criteria("content").matches(keyword)))
                .and(new Criteria("status").is(1));

        Query query = new CriteriaQuery(criteria).setPageable(org.springframework.data.domain.PageRequest.of(page, size));
        SearchHits<PostDocument> searchHits = elasticsearchOperations.search(query, PostDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * 索引单个帖子（发布、更新时调用）
     */
    public void indexPost(Post post) {
        try {
            PostDocument doc = convertToDocument(post);
            searchRepository.save(doc);
            log.debug("索引帖子成功: postId={}", post.getId());
        } catch (Exception e) {
            log.error("索引帖子失败: postId={}, error={}", post.getId(), e.getMessage());
        }
    }

    /**
     * 删除帖子索引
     */
    public void deletePostIndex(Long postId) {
        try {
            searchRepository.deleteById(postId);
            log.debug("删除帖子索引成功: postId={}", postId);
        } catch (Exception e) {
            log.error("删除帖子索引失败: postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 全量同步MySQL数据到ES
     * 用于初始化或数据修复
     */
    public void syncAllPosts() {
        log.info("开始全量同步帖子到ES...");
        
        // 使用 MyBatis-Plus 查询已发布且未删除的帖子
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Post::getStatus, 1)
                    .eq(Post::getDeleted, 0)
                    .orderByDesc(Post::getCreateTime);
        
        Page<Post> page = new Page<>(1, 10000);
        IPage<Post> posts = postMapper.selectPage(page, queryWrapper);

        List<PostDocument> documents = posts.getRecords().stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

        searchRepository.saveAll(documents);
        log.info("全量同步完成: 共同步{}条帖子", documents.size());
    }

    /**
     * Post实体转ES文档
     */
    private PostDocument convertToDocument(Post post) {
        PostDocument doc = new PostDocument();
        doc.setId(post.getId());
        doc.setTitle(post.getTitle());
        doc.setContent(post.getContent());
        doc.setAuthorId(post.getAuthorId());
        doc.setCategory(post.getCategory());
        doc.setTags(post.getTags());
        doc.setLikeCount(post.getLikeCount());
        doc.setCommentCount(post.getCommentCount());
        doc.setViewCount(post.getViewCount());
        doc.setStatus(post.getStatus());
        doc.setCreateTime(post.getCreateTime() != null ? post.getCreateTime().toString() : null);
        return doc;
    }
}
