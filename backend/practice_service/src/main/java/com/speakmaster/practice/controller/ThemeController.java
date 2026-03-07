package com.speakmaster.practice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.practice.dto.ThemeDTO;
import com.speakmaster.practice.service.IThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主题控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class ThemeController {

    private final IThemeService themeService;

    /**
     * 获取主题列表 (用户端)
     */
    @GetMapping("/user/practice/themes")
    public Result<Page<ThemeDTO>> getThemeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ThemeDTO> themes = themeService.getThemeList(page, size);
        return Result.success(themes);
    }

    /**
     * 获取主题详情 (用户端)
     */
    @GetMapping("/user/practice/themes/{id}")
    public Result<ThemeDTO> getThemeById(@PathVariable Long id) {
        ThemeDTO theme = themeService.getThemeById(id);
        return Result.success(theme);
    }

    /**
     * 获取热门主题 (用户端)
     */
    @GetMapping("/user/practice/themes/popular")
    public Result<List<ThemeDTO>> getPopularThemes() {
        List<ThemeDTO> themes = themeService.getPopularThemes();
        return Result.success(themes);
    }

    /**
     * 创建主题 (管理端)
     */
    @PostMapping("/admin/practice/themes")
    public Result<ThemeDTO> createTheme(@RequestBody ThemeDTO themeDTO) {
        ThemeDTO theme = themeService.createTheme(themeDTO);
        return Result.success(theme);
    }

    /**
     * 更新主题 (管理端)
     */
    @PutMapping("/admin/practice/themes/{id}")
    public Result<ThemeDTO> updateTheme(
            @PathVariable Long id,
            @RequestBody ThemeDTO themeDTO) {
        ThemeDTO theme = themeService.updateTheme(id, themeDTO);
        return Result.success(theme);
    }

    /**
     * 删除主题 (管理端)
     */
    @DeleteMapping("/admin/practice/themes/{id}")
    public Result<Void> deleteTheme(@PathVariable Long id) {
        themeService.deleteTheme(id);
        return Result.success();
    }
}
