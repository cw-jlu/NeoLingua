package com.speakmaster.user.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 签到服务接口
 * 
 * @author SpeakMaster
 */
public interface ISignInService {

    /**
     * 每日签到
     */
    void signIn(Long userId);

    /**
     * 获取签到状态
     */
    SignInStatus getSignInStatus(Long userId);

    /**
     * 获取签到日历
     */
    List<Integer> getSignInCalendar(Long userId, LocalDate date);

    /**
     * 签到状态DTO
     */
    class SignInStatus {
        private Boolean todaySigned;
        private Integer continuousDays;
        private Integer monthDays;
        private Integer totalDays;

        public Boolean getTodaySigned() {
            return todaySigned;
        }

        public void setTodaySigned(Boolean todaySigned) {
            this.todaySigned = todaySigned;
        }

        public Integer getContinuousDays() {
            return continuousDays;
        }

        public void setContinuousDays(Integer continuousDays) {
            this.continuousDays = continuousDays;
        }

        public Integer getMonthDays() {
            return monthDays;
        }

        public void setMonthDays(Integer monthDays) {
            this.monthDays = monthDays;
        }

        public Integer getTotalDays() {
            return totalDays;
        }

        public void setTotalDays(Integer totalDays) {
            this.totalDays = totalDays;
        }
    }
}
