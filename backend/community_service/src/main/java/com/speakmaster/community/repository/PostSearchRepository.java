package com.speakmaster.community.repository;

import com.speakmaster.community.document.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 帖子ES搜索Repository
 * 
 * @author SpeakMaster
 */
@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Long> {

    /**
     * 按分类查�?
     */
    Page<PostDocument> findByCategory(String category, Pageable pageable);

    /**
     * 按状态查�?
     */
    Page<PostDocument> findByStatus(Integer status, Pageable pageable);
}
