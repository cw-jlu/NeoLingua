package com.speakmaster.community.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * 帖子ES文档
 * 用于Elasticsearch全文搜索
 * 
 * @author SpeakMaster
 */
@Data
@Document(indexName = "posts")
@Setting(shards = 1, replicas = 0)
public class PostDocument {

    @Id
    private Long id;

    /** 标题 - 使用标准分词�?(如需中文分词，请在ES中安装IK插件后改为ik_max_word) */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String title;

    /** 内容 - 使用标准分词�?(如需中文分词，请在ES中安装IK插件后改为ik_max_word) */
    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String content;

    /** 作者ID */
    @Field(type = FieldType.Long)
    private Long authorId;

    /** 分类 */
    @Field(type = FieldType.Keyword)
    private String category;

    /** 标签 */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String tags;

    /** 点赞�?*/
    @Field(type = FieldType.Integer)
    private Integer likeCount;

    /** 评论�?*/
    @Field(type = FieldType.Integer)
    private Integer commentCount;

    /** 浏览�?*/
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /** 状�?*/
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 创建时间 */
    @Field(type = FieldType.Keyword)
    private String createTime;
}
