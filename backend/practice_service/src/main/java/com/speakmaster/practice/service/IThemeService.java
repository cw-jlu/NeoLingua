package com.speakmaster.practice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.practice.dto.ThemeDTO;
import com.speakmaster.practice.entity.Theme;

import java.util.List;

/**
 * 主题服务接口
 * 
 * @author SpeakMaster
 */
public interface IThemeService extends IService<Theme> {

    /**
     * 分页查询主题列表
     */
    Page<ThemeDTO> getThemeList(int page, int size);

    /**
     * 获取主题详情
     */
    ThemeDTO getThemeById(Long themeId);

    /**
     * 获取热门主题
     */
    List<ThemeDTO> getPopularThemes();

    /**
     * 创建主题 (管理端)
     */
    ThemeDTO createTheme(ThemeDTO themeDTO);

    /**
     * 更新主题 (管理端)
     */
    ThemeDTO updateTheme(Long themeId, ThemeDTO themeDTO);

    /**
     * 删除主题 (管理端)
     */
    void deleteTheme(Long themeId);

    /**
     * 增加使用次数
     */
    void incrementUseCount(Long themeId);
}
