CREATE TABLE quizzes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       TEXT        NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE questions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id              UUID        NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    text                 TEXT        NOT NULL,
    type                 TEXT        NOT NULL CHECK (type IN ('single', 'multiple')),
    difficulty           TEXT        NOT NULL DEFAULT 'medium',
    points               INT         NOT NULL DEFAULT 1,
    after_answer_comment TEXT,
    order_number         INT         NOT NULL,
    UNIQUE (quiz_id, order_number)
);

CREATE TABLE options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID    NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    option_key  TEXT    NOT NULL,
    text        TEXT    NOT NULL,
    is_correct  BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (question_id, option_key)
);

CREATE TABLE rooms (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                   TEXT        NOT NULL UNIQUE,
    quiz_id                UUID        NOT NULL REFERENCES quizzes (id),
    organizer_token        TEXT        NOT NULL,
    status                 TEXT        NOT NULL,
    current_question_index INT         NOT NULL DEFAULT -1,
    manual_mode            BOOLEAN     NOT NULL DEFAULT TRUE,
    annoying_mode_enabled  BOOLEAN     NOT NULL DEFAULT TRUE,
    question_started_at    TIMESTAMPTZ,
    question_ends_at       TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at             TIMESTAMPTZ,
    finished_at            TIMESTAMPTZ
);

CREATE TABLE players (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id       UUID        NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    name          TEXT        NOT NULL,
    session_id    TEXT        NOT NULL,
    score         INT         NOT NULL DEFAULT 0,
    joined_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (room_id, session_id)
);

CREATE INDEX idx_players_room ON players (room_id);

CREATE TABLE answers (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id          UUID        NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    player_id        UUID        NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    question_id      UUID        NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    selected_options TEXT        NOT NULL,
    is_correct       BOOLEAN     NOT NULL,
    points_earned    INT         NOT NULL DEFAULT 0,
    answered_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    answer_time_ms   BIGINT      NOT NULL,
    UNIQUE (room_id, player_id, question_id)
);

CREATE INDEX idx_answers_room_question ON answers (room_id, question_id);
