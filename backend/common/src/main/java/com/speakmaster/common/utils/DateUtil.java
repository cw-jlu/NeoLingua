package com.speakmaster.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具�?
 * 提供常用的日期时间操�?
 * 
 * @author SpeakMaster
 */
public class DateUtil {

    /**
     * 默认日期时间格式
     */
    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式
     */
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 默认时间格式
     */
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    /**
     * 格式化日期时�?
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DEFAULT_DATETIME_PATTERN);
    }

    /**
     * 格式化日期时�?(自定义格�?
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日�?
     */
    public static String formatDate(LocalDate date) {
        return formatDate(date, DEFAULT_DATE_PATTERN);
    }

    /**
     * 格式化日�?(自定义格�?
     */
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期时间字符�?
     */
    public static LocalDateTime parse(String dateTimeStr) {
        return parse(dateTimeStr, DEFAULT_DATETIME_PATTERN);
    }

    /**
     * 解析日期时间字符�?(自定义格�?
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期字符�?
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, DEFAULT_DATE_PATTERN);
    }

    /**
     * 解析日期字符�?(自定义格�?
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取当前日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前日期
     */
    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * 计算两个日期之间的天�?
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的小时数
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的分钟数
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个日期时间之间的秒�?
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 判断是否是今�?
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }

    /**
     * 判断是否是今�?
     */
    public static boolean isToday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 获取指定日期的开始时�?(00:00:00)
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 获取指定日期的结束时�?(23:59:59)
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    /**
     * 增加天数
     */
    public static LocalDate plusDays(LocalDate date, long days) {
        return date.plusDays(days);
    }

    /**
     * 减少天数
     */
    public static LocalDate minusDays(LocalDate date, long days) {
        return date.minusDays(days);
    }

    /**
     * 增加小时
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        return dateTime.plusHours(hours);
    }

    /**
     * 减少小时
     */
    public static LocalDateTime minusHours(LocalDateTime dateTime, long hours) {
        return dateTime.minusHours(hours);
    }
}
