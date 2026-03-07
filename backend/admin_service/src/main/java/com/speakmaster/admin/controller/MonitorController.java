package com.speakmaster.admin.controller;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/admin/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final HealthEndpoint healthEndpoint;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthData = new HashMap<>();
        try {
            HealthComponent health = healthEndpoint.health();
            healthData.put("status", health.getStatus().getCode());
            Map<String, String> components = new HashMap<>();
            if (health instanceof CompositeHealth compositeHealth) {
                compositeHealth.getComponents().forEach((name, component) ->
                        components.put(name, component.getStatus().getCode()));
            }
            healthData.put("components", components);
        } catch (Exception e) {
            log.warn("获取健康检查数据失败");
            healthData.put("status", Status.UNKNOWN.getCode());
        }
        return Result.success(healthData);
    }

    /**
     * 系统指标
     */
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        Map<String, Object> metricsData = new HashMap<>();

        // JVM内存
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        metricsData.put("jvmHeapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        metricsData.put("jvmHeapMax", memoryBean.getHeapMemoryUsage().getMax());
        metricsData.put("jvmNonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());

        // 线程
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metricsData.put("threadCount", threadBean.getThreadCount());
        metricsData.put("peakThreadCount", threadBean.getPeakThreadCount());

        // CPU
        metricsData.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        metricsData.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

        return Result.success(metricsData);
    }

    /**
     * 服务状态
     */
    @GetMapping("/services")
    public Result<Map<String, Object>> services() {
        // 返回各微服务的配置信息（实际状态需要通过服务发现获取）
        Map<String, Object> servicesData = new HashMap<>();
        String[][] serviceList = {
                {"user-service", "http://localhost:8081"},
                {"practice-service", "http://localhost:8082"},
                {"meeting-service", "http://localhost:8083"},
                {"community-service", "http://localhost:8084"},
                {"analysis-service", "http://localhost:8085"},
                {"notification-service", "http://localhost:8086"},
                {"admin-service", "http://localhost:8087"},
                {"ai-gateway", "http://localhost:8088"},
                {"ai-service", "http://localhost:8089"}
        };

        for (String[] svc : serviceList) {
            Map<String, String> info = new HashMap<>();
            info.put("name", svc[0]);
            info.put("url", svc[1]);
            info.put("status", "UNKNOWN");
            servicesData.put(svc[0], info);
        }
        return Result.success(servicesData);
    }

    /**
     * 性能监控
     */
    @GetMapping("/performance")
    public Result<Map<String, Object>> performance() {
        Map<String, Object> perfData = new HashMap<>();

        // 运行时间
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        perfData.put("uptimeMs", uptimeMs);
        perfData.put("uptime", formatUptime(uptimeMs));

        // 内存使用率
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        perfData.put("memoryUsagePercent", (double) (totalMemory - freeMemory) / totalMemory * 100);
        perfData.put("totalMemoryMB", totalMemory / (1024 * 1024));
        perfData.put("usedMemoryMB", (totalMemory - freeMemory) / (1024 * 1024));

        // GC信息
        long gcCount = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount()).sum();
        long gcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionTime()).sum();
        perfData.put("gcCount", gcCount);
        perfData.put("gcTimeMs", gcTime);

        return Result.success(perfData);
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptimeMs) {
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        long hours = (uptimeMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%d天%d小时%d分钟", days, hours, minutes);
    }
}
