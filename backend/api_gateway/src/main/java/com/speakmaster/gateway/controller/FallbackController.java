package com.speakmaster.gateway.controller;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 熔断降级处理器
 * 当Sentinel触发限流/熔断时返回友好的错误信息
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/blocked")
    public Result<?> blocked() {
        log.warn("请求被Sentinel限流或熔断");
        return Result.error(429, "服务繁忙，请稍后重试");
    }
}
