-- ============================================================
-- 增量迁移：chat_session 补 has_result 列
-- 背景：新主诉判定的双信号 = has_result=1 或 status=closed（见《数据库设计.md》§2、
--       《总体架构与链路设计.md》链路 C 对齐点）。设计文档里本就有该字段，建表脚本漏建。
-- 影响：不执行时，新代码插入会话会报 Unknown column 'has_result'，每轮对话直接失败。
-- 执行：已在旧库上跑过 mysql_init.sql 的环境执行本脚本；全新初始化无需执行（脚本已含该列）。
-- ============================================================

ALTER TABLE `chat_session`
    ADD COLUMN `has_result` TINYINT NOT NULL DEFAULT 0
        COMMENT '0/1 已出导诊结论（新主诉判定双信号之一）' AFTER `ask_round`;

-- 历史会话回填：已有导诊记录的会话视为已出结论
UPDATE `chat_session` s
SET s.`has_result` = 1
WHERE EXISTS (SELECT 1 FROM `guide_record` g WHERE g.`session_id` = s.`id` AND g.`deleted` = 0);

-- 新增运行时参数：追问轮数上限（超限强制出低置信度结论）
INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `remark`, `deleted`, `created_at`, `updated_at`)
VALUES ('c07', 'chat.ask.max.rounds', '3', '追问轮数上限（超限强制出低置信度结论）', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);
