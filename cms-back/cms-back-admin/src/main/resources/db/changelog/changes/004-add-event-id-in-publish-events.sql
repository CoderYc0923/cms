-- 添加事件ID到发布事件表

ALTER TABLE publish_events
ADD COLUMN event_id VARCHAR(64) NOT NULL DEFAULT '' AFTER id,
ADD UNIQUE KEY uk_publish_events_event_id (event_id);