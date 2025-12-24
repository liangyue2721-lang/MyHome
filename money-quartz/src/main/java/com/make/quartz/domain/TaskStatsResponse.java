package com.make.quartz.domain;

import com.make.common.core.domain.BaseEntity;

/**
 * 任务统计响应类
 *
 * @author erqi
 * @date 2025-12-24
 */
public class TaskStatsResponse {

    private TaskStats taskStats;
    private Integer executingPercentage;

    public TaskStatsResponse() {
    }

    public TaskStatsResponse(TaskStats taskStats, Integer executingPercentage) {
        this.taskStats = taskStats;
        this.executingPercentage = executingPercentage;
    }

    public TaskStats getTaskStats() {
        return taskStats;
    }

    public void setTaskStats(TaskStats taskStats) {
        this.taskStats = taskStats;
    }

    public Integer getExecutingPercentage() {
        return executingPercentage;
    }

    public void setExecutingPercentage(Integer executingPercentage) {
        this.executingPercentage = executingPercentage;
    }

    /**
     * 任务统计内部类
     */
    public static class TaskStats {
        private Integer pending;
        private Integer completed;
        private Integer executing;

        public TaskStats() {
        }

        public TaskStats(Integer pending, Integer completed, Integer executing) {
            this.pending = pending;
            this.completed = completed;
            this.executing = executing;
        }

        public Integer getPending() {
            return pending;
        }

        public void setPending(Integer pending) {
            this.pending = pending;
        }

        public Integer getCompleted() {
            return completed;
        }

        public void setCompleted(Integer completed) {
            this.completed = completed;
        }

        public Integer getExecuting() {
            return executing;
        }

        public void setExecuting(Integer executing) {
            this.executing = executing;
        }
    }
}