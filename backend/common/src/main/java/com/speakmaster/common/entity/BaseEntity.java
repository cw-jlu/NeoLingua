package com.speakmaster.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 * 所有实体类都应继承此类，包含公共字段
 * 
 * @author SpeakMaster
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记 (0-未删除, 1-已删除)
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted = 0;

    /**
     * 逻辑删除
     */
    public void markDeleted() {
        this.deleted = 1;
    }

    /**
     * 是否已删除
     */
    public boolean checkDeleted() {
        return this.deleted != null && this.deleted == 1;
    }
}
