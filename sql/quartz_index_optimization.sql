-- 定时任务日志表索引优化脚本
-- 为解决sys_job_log表查询缓慢问题

-- 为job_group字段添加索引
CREATE INDEX idx_sys_job_log_job_group ON sys_job_log (job_group);

-- 为job_name字段添加前缀索引（考虑到该字段在查询中使用LIKE '%keyword%'模式，前缀索引对性能提升有限，但仍有一定帮助）
CREATE INDEX idx_sys_job_log_job_name ON sys_job_log (job_name);

-- 为组合查询条件创建复合索引
CREATE INDEX idx_sys_job_log_group_name ON sys_job_log (job_group, job_name);

-- 为创建时间添加索引，用于时间范围查询
CREATE INDEX idx_sys_job_log_create_time ON sys_job_log (create_time);

-- 定期清理过期的日志数据，建议添加到定时任务中
-- DELETE FROM sys_job_log WHERE create_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
