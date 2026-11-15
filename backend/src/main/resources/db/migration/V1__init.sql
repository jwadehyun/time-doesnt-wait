-- Time Doesn't Wait: initial schema. Mirrors the phone's SQLite tables, plus user ownership.
-- Every synced row carries id (UUIDv7 made on the phone), updated_at (client time of the last change),
-- deleted_at (soft delete, so deletions sync) and server_seq (assigned here on every write, used by pull).

CREATE SEQUENCE server_seq;

CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    display_name  TEXT        NOT NULL DEFAULT '',
    locale        TEXT        NOT NULL DEFAULT 'en' CHECK (locale IN ('en', 'ko')),
    time_zone     TEXT        NOT NULL DEFAULT 'UTC',
    day_start     SMALLINT    NOT NULL DEFAULT 9  CHECK (day_start BETWEEN 0 AND 23),
    day_end       SMALLINT    NOT NULL DEFAULT 20 CHECK (day_end BETWEEN 0 AND 23),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE linked_account (
    id               UUID PRIMARY KEY,
    user_id          UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    provider         TEXT        NOT NULL CHECK (provider IN ('GOOGLE', 'KAKAO')),
    provider_subject TEXT        NOT NULL,
    email            TEXT,
    linked_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_subject)
);

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE activity (
    id          UUID PRIMARY KEY,
    user_id     UUID    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name        TEXT    NOT NULL,
    name_key    TEXT,
    icon        TEXT    NOT NULL,
    keywords    TEXT    NOT NULL DEFAULT '',
    sort_order  INT     NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    archived    BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at  BIGINT  NOT NULL,
    deleted_at  BIGINT,
    server_seq  BIGINT  NOT NULL DEFAULT nextval('server_seq')
);

CREATE TABLE vibe (
    id          UUID PRIMARY KEY,
    user_id     UUID    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    label       TEXT    NOT NULL,
    label_key   TEXT,
    kaomoji     TEXT    NOT NULL,
    sort_order  INT     NOT NULL,
    archived    BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at  BIGINT  NOT NULL,
    deleted_at  BIGINT,
    server_seq  BIGINT  NOT NULL DEFAULT nextval('server_seq')
);

CREATE TABLE entry (
    id               UUID PRIMARY KEY,
    user_id          UUID     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    local_date       DATE     NOT NULL,
    hour             SMALLINT NOT NULL CHECK (hour BETWEEN 0 AND 23),
    activity_id      UUID,
    vibe_id          UUID,
    memo             TEXT     NOT NULL DEFAULT '' CHECK (char_length(memo) <= 100),
    source           TEXT     NOT NULL CHECK (source IN ('NOTIFICATION_REPLY', 'QUICK_BUTTON', 'APP', 'BACKFILL')),
    category_guessed BOOLEAN  NOT NULL DEFAULT FALSE,
    logged_at        BIGINT   NOT NULL,
    updated_at       BIGINT   NOT NULL,
    deleted_at       BIGINT,
    server_seq       BIGINT   NOT NULL DEFAULT nextval('server_seq')
);
-- At most one live entry per user per day per hour.
CREATE UNIQUE INDEX entry_slot ON entry (user_id, local_date, hour) WHERE deleted_at IS NULL;

CREATE TABLE goal (
    id            UUID PRIMARY KEY,
    user_id       UUID    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name          TEXT    NOT NULL,
    target_hours  INT     NOT NULL CHECK (target_hours > 0),
    achieved      BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order    INT     NOT NULL,
    updated_at    BIGINT  NOT NULL,
    deleted_at    BIGINT,
    server_seq    BIGINT  NOT NULL DEFAULT nextval('server_seq')
);

CREATE TABLE goal_log (
    id          UUID PRIMARY KEY,
    user_id     UUID     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    goal_id     UUID     NOT NULL,
    local_date  DATE     NOT NULL,
    hour        SMALLINT NOT NULL CHECK (hour BETWEEN 0 AND 23),
    updated_at  BIGINT   NOT NULL,
    deleted_at  BIGINT,
    server_seq  BIGINT   NOT NULL DEFAULT nextval('server_seq')
);
CREATE UNIQUE INDEX goal_log_slot ON goal_log (goal_id, local_date, hour) WHERE deleted_at IS NULL;

CREATE TABLE rating_dimension (
    id          UUID PRIMARY KEY,
    user_id     UUID    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name        TEXT    NOT NULL,
    name_key    TEXT,
    kind        TEXT    NOT NULL CHECK (kind IN ('STARS_5', 'HEARTS_5', 'SEGMENTS_10')),
    sort_order  INT     NOT NULL,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    archived    BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at  BIGINT  NOT NULL,
    deleted_at  BIGINT,
    server_seq  BIGINT  NOT NULL DEFAULT nextval('server_seq')
);

CREATE TABLE daily_review (
    id              UUID PRIMARY KEY,
    user_id         UUID     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    local_date      DATE     NOT NULL,
    overall_rating  SMALLINT NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    memo            TEXT     NOT NULL DEFAULT '',
    saved_at        BIGINT   NOT NULL,
    updated_at      BIGINT   NOT NULL,
    deleted_at      BIGINT,
    server_seq      BIGINT   NOT NULL DEFAULT nextval('server_seq')
);
CREATE UNIQUE INDEX daily_review_date ON daily_review (user_id, local_date) WHERE deleted_at IS NULL;

CREATE TABLE review_rating (
    id            UUID PRIMARY KEY,
    user_id       UUID     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    review_id     UUID     NOT NULL,
    dimension_id  UUID     NOT NULL,
    value         SMALLINT NOT NULL,
    note          TEXT,
    updated_at    BIGINT   NOT NULL,
    deleted_at    BIGINT,
    server_seq    BIGINT   NOT NULL DEFAULT nextval('server_seq')
);

CREATE TABLE checkin_schedule (
    user_id     UUID     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    weekday     SMALLINT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    hours_mask  INT      NOT NULL CHECK (hours_mask BETWEEN 0 AND 16777215),
    updated_at  BIGINT   NOT NULL,
    server_seq  BIGINT   NOT NULL DEFAULT nextval('server_seq'),
    PRIMARY KEY (user_id, weekday)
);

-- Pull = "every row of this user with server_seq > ?", per table.
CREATE INDEX activity_pull         ON activity         (user_id, server_seq);
CREATE INDEX vibe_pull             ON vibe             (user_id, server_seq);
CREATE INDEX entry_pull            ON entry            (user_id, server_seq);
CREATE INDEX goal_pull             ON goal             (user_id, server_seq);
CREATE INDEX goal_log_pull         ON goal_log         (user_id, server_seq);
CREATE INDEX rating_dimension_pull ON rating_dimension (user_id, server_seq);
CREATE INDEX daily_review_pull     ON daily_review     (user_id, server_seq);
CREATE INDEX review_rating_pull    ON review_rating    (user_id, server_seq);
CREATE INDEX checkin_schedule_pull ON checkin_schedule (user_id, server_seq);
