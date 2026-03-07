package com.speakmaster.meeting.dto;

import lombok.Data;

/**
 * 好友DTO
 * 
 * @author SpeakMaster
 */
@Data
public class FriendDTO {
    
    /** 好友关系ID */
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 好友ID */
    private Long friendId;
    
    /** 状�?(0-待确�?1-已接�?2-已拒�? */
    private Integer status;
    
    /** 备注名称 */
    private String remark;
    
    /** 好友用户�?*/
    private String friendUsername;
    
    /** 好友昵称 */
    private String friendNickname;
    
    /** 好友头像 */
    private String friendAvatar;
}
