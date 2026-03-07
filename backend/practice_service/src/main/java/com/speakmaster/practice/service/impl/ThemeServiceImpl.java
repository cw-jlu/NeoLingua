package com.speakmaster.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import com.speakmaster.practice.dto.ThemeDTO;
import com.speakmaster.practice.entity.Theme;
import com.speakmaster.practice.mapper.ThemeMapper;
import com.speakmaster.practice.service.IThemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 主题服务实现类
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeServiceImpl extends ServiceImpl<ThemeMapper, Theme> implements IThemeService {

    private final RedisUtil redisUtil;

    @Override
    @Cacheable(value = "theme_list", key = "#page + '_' + #size")
    public Page<ThemeDTO> getThemeList(int page, int size) {
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Theme::getStatus, 1)
                .eq(Theme::getDeleted, 0)
                .orderByAsc(Theme::getSortOrder);
        
        Page<Theme> pageRequest = new Page<>(page + 1, size);
        Page<Theme> themes = this.page(pageRequest, wrapper);
        
        return (Page<ThemeDTO>) themes.convert(this::convertToDTO);
    }

    @Override
    @Cacheable(value = "theme", key = "#themeId")
    public ThemeDTO getThemeById(Long themeId) {
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Theme::getId, themeId)
                .eq(Theme::getDeleted, 0);
        Theme theme = this.getOne(wrapper);
        
        if (theme == null) {
            throw new BusinessException(ErrorCode.THEME_NOT_FOUND);
        }
        return convertToDTO(theme);
    }

    @Override
    @Cacheable(value = "theme_popular", key = "'popular'")
    public List<ThemeDTO> getPopularThemes() {
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Theme::getStatus, 1)
                .eq(Theme::getDeleted, 0)
                .orderByDesc(Theme::getUseCount)
                .last("LIMIT 10");
        
        List<Theme> themes = this.list(wrapper);
        return themes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"theme_list", "theme_popular"}, allEntries = true)
    public ThemeDTO createTheme(ThemeDTO themeDTO) {
        Theme theme = new Theme();
        theme.setName(themeDTO.getName());
        theme.setDescription(themeDTO.getDescription());
        theme.setCover(themeDTO.getCover());
        theme.setCategory(themeDTO.getCategory() != null ? Integer.parseInt(themeDTO.getCategory()) : 1);
        theme.setDifficulty(themeDTO.getDifficulty());
        theme.setTags(themeDTO.getTags());
        theme.setSortOrder(themeDTO.getSortOrder());
        theme.setStatus(0); // 默认草稿状态

        this.save(theme);
        log.info("创建主题成功: themeId={}", theme.getId());
        return convertToDTO(theme);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"theme", "theme_list", "theme_popular"}, allEntries = true)
    public ThemeDTO updateTheme(Long themeId, ThemeDTO themeDTO) {
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Theme::getId, themeId)
                .eq(Theme::getDeleted, 0);
        Theme theme = this.getOne(wrapper);
        
        if (theme == null) {
            throw new BusinessException(ErrorCode.THEME_NOT_FOUND);
        }

        if (themeDTO.getName() != null) theme.setName(themeDTO.getName());
        if (themeDTO.getDescription() != null) theme.setDescription(themeDTO.getDescription());
        if (themeDTO.getCover() != null) theme.setCover(themeDTO.getCover());
        if (themeDTO.getCategory() != null) theme.setCategory(Integer.parseInt(themeDTO.getCategory()));
        if (themeDTO.getDifficulty() != null) theme.setDifficulty(themeDTO.getDifficulty());
        if (themeDTO.getTags() != null) theme.setTags(themeDTO.getTags());
        if (themeDTO.getSortOrder() != null) theme.setSortOrder(themeDTO.getSortOrder());
        if (themeDTO.getStatus() != null) theme.setStatus(themeDTO.getStatus());

        this.updateById(theme);
        log.info("更新主题成功: themeId={}", themeId);
        return convertToDTO(theme);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"theme", "theme_list", "theme_popular"}, allEntries = true)
    public void deleteTheme(Long themeId) {
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Theme::getId, themeId)
                .eq(Theme::getDeleted, 0);
        Theme theme = this.getOne(wrapper);
        
        if (theme == null) {
            throw new BusinessException(ErrorCode.THEME_NOT_FOUND);
        }

        theme.markDeleted();
        this.updateById(theme);
        log.info("删除主题成功: themeId={}", themeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long themeId) {
        LambdaQueryWrapper<Theme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Theme::getId, themeId)
                .eq(Theme::getDeleted, 0);
        Theme theme = this.getOne(wrapper);
        
        if (theme != null) {
            theme.setUseCount(theme.getUseCount() + 1);
            this.updateById(theme);
        }
    }

    /**
     * 转换为DTO
     */
    private ThemeDTO convertToDTO(Theme theme) {
        ThemeDTO dto = new ThemeDTO();
        dto.setId(theme.getId());
        dto.setName(theme.getName());
        dto.setDescription(theme.getDescription());
        dto.setCover(theme.getCover());
        dto.setCategory(theme.getCategory() != null ? theme.getCategory().toString() : null);
        dto.setDifficulty(theme.getDifficulty());
        dto.setTags(theme.getTags());
        dto.setUseCount(theme.getUseCount() != null ? theme.getUseCount().intValue() : 0);
        dto.setSortOrder(theme.getSortOrder());
        dto.setStatus(theme.getStatus());
        return dto;
    }
}
