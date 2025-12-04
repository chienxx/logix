-- 创建数据库
CREATE DATABASE IF NOT EXISTS logix;

USE logix;

-- 运行日志表
CREATE TABLE IF NOT EXISTS run_logs
(
    event_time   DateTime64(3) COMMENT '日志时间',
    app_name     String COMMENT '应用名称',
    env          String COMMENT '环境',
    server_ip    String COMMENT '服务器IP',
    seq          UInt64 COMMENT '序列号',
    log_level    Enum8('TRACE' = 1, 'DEBUG' = 2, 'INFO' = 3, 'WARN' = 4, 'ERROR' = 5) COMMENT '日志级别',
    content      String COMMENT '日志内容',
    class_name   String COMMENT '类名',
    method_name  String COMMENT '方法名',
    thread_name  String COMMENT '线程名',
    trace_id     String COMMENT '链路ID',
    insert_time  DateTime DEFAULT now() COMMENT '插入时间'
    )
    ENGINE = MergeTree
    PARTITION BY toYYYYMM(event_time)
    ORDER BY (app_name, env, event_time, seq, trace_id)
    TTL insert_time + INTERVAL 180 DAY
    SETTINGS index_granularity = 8192;

-- 追踪日志表
CREATE TABLE IF NOT EXISTS trace_logs
(
    event_time   DateTime64(3) COMMENT '日志时间',
    app_name     String COMMENT '应用名称',
    env          String COMMENT '环境',
    server_ip    String COMMENT '服务器IP',
    trace_id     String COMMENT '链路ID',
    method_name  String COMMENT '方法签名',
    position     Enum8('<' = 1, '>' = 2) COMMENT '链路位置',
    depth        UInt16 COMMENT '调用深度',
    insert_time  DateTime DEFAULT now() COMMENT '插入时间'
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (trace_id, event_time, position)
TTL insert_time + INTERVAL 180 DAY
SETTINGS index_granularity = 8192;
