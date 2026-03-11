-- =============================================
-- Job Tracker 数据库初始化脚本
-- =============================================

-- =============================================
-- 用户表
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(255) PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    email           VARCHAR(255) UNIQUE,
    display_name    VARCHAR(255),
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- =============================================
-- 看板表
-- =============================================
CREATE TABLE IF NOT EXISTS board (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL DEFAULT 'My Job Tracker',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_board_user_id ON board(user_id);

-- =============================================
-- 看板列（状态列）
-- =============================================
CREATE TABLE IF NOT EXISTS "kanban_column" (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id            UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_default          BOOLEAN NOT NULL DEFAULT true,
    custom_attributes   TEXT,
    CONSTRAINT fk_column_board FOREIGN KEY (board_id) REFERENCES board(id)
);

CREATE INDEX IF NOT EXISTS idx_column_board_id ON "kanban_column"(board_id);

-- =============================================
-- 职位卡片表
-- =============================================
CREATE TABLE IF NOT EXISTS job_card (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id        UUID NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    status_id       UUID NOT NULL REFERENCES "kanban_column"(id),
    job_title       VARCHAR(500) NOT NULL,
    company_name    VARCHAR(255) NOT NULL,
    job_link        VARCHAR(1000),
    source_platform VARCHAR(100),
    expired         BOOLEAN DEFAULT false,
    job_location    VARCHAR(255),
    description     TEXT,
    applied_time    TIMESTAMP WITH TIME ZONE,
    tags            TEXT,
    comments        TEXT,
    extra           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_job_card_board   FOREIGN KEY (board_id) REFERENCES board(id),
    CONSTRAINT fk_job_card_status  FOREIGN KEY (status_id) REFERENCES "kanban_column"(id)
);

CREATE INDEX IF NOT EXISTS idx_job_card_board_id   ON job_card(board_id);
CREATE INDEX IF NOT EXISTS idx_job_card_deleted_at ON job_card(deleted_at);

-- =============================================
-- 初始化完成提示
-- =============================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '数据库初始化完成!';
    RAISE NOTICE '已创建表: users, board, kanban_column, job_card';
    RAISE NOTICE '========================================';
END $$;