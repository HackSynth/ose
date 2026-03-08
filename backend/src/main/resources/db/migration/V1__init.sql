create table users (
    id bigint primary key auto_increment,
    username varchar(64) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(100) not null,
    role varchar(20) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table system_settings (
    id bigint primary key auto_increment,
    exam_date date not null,
    passing_score int not null,
    weekly_study_hours int not null,
    learning_preference varchar(255) not null,
    review_intervals varchar(64) not null,
    daily_session_minutes int not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table knowledge_points (
    id bigint primary key auto_increment,
    code varchar(64) not null unique,
    name varchar(120) not null,
    level int not null,
    mastery_level int not null,
    weight int not null,
    note text null,
    sort_order int not null,
    parent_id bigint null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_knowledge_parent foreign key (parent_id) references knowledge_points(id)
);

create table questions (
    id bigint primary key auto_increment,
    type varchar(32) not null,
    title varchar(200) not null,
    content text not null,
    correct_answer varchar(16) null,
    explanation text null,
    reference_answer text null,
    question_year int not null,
    difficulty int not null,
    source varchar(120) not null,
    tags varchar(255) not null,
    score decimal(10,2) not null,
    active boolean not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table question_options (
    id bigint primary key auto_increment,
    question_id bigint not null,
    option_key varchar(4) not null,
    content text not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_question_options_question foreign key (question_id) references questions(id)
);

create table question_knowledge_rel (
    question_id bigint not null,
    knowledge_point_id bigint not null,
    primary key (question_id, knowledge_point_id),
    constraint fk_qkr_question foreign key (question_id) references questions(id),
    constraint fk_qkr_knowledge foreign key (knowledge_point_id) references knowledge_points(id)
);

create table study_plans (
    id bigint primary key auto_increment,
    name varchar(120) not null,
    exam_date date not null,
    start_date date not null,
    end_date date not null,
    total_hours int not null,
    status varchar(20) not null,
    setting_snapshot varchar(255) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table study_tasks (
    id bigint primary key auto_increment,
    plan_id bigint not null,
    phase varchar(20) not null,
    task_type varchar(20) not null,
    status varchar(20) not null,
    title varchar(160) not null,
    description text null,
    knowledge_point_id bigint null,
    scheduled_date date not null,
    estimated_minutes int not null,
    priority int not null,
    progress int not null,
    postponed_to date null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_study_task_plan foreign key (plan_id) references study_plans(id),
    constraint fk_study_task_knowledge foreign key (knowledge_point_id) references knowledge_points(id)
);

create table practice_sessions (
    id bigint primary key auto_increment,
    session_type varchar(20) not null,
    question_type varchar(32) not null,
    status varchar(20) not null,
    knowledge_point_id bigint null,
    question_count int not null,
    started_at datetime(6) not null,
    submitted_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_practice_session_knowledge foreign key (knowledge_point_id) references knowledge_points(id)
);

create table practice_records (
    id bigint primary key auto_increment,
    session_id bigint not null,
    question_id bigint not null,
    user_answer text null,
    auto_score decimal(10,2) null,
    subjective_score decimal(10,2) null,
    result varchar(20) null,
    favorite boolean not null,
    marked_unknown boolean not null,
    added_to_review boolean not null,
    duration_seconds int not null,
    submitted_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_practice_record_session foreign key (session_id) references practice_sessions(id),
    constraint fk_practice_record_question foreign key (question_id) references questions(id)
);

create table mistake_records (
    id bigint primary key auto_increment,
    question_id bigint not null,
    practice_record_id bigint not null,
    knowledge_point_id bigint null,
    reason_type varchar(40) not null,
    review_status varchar(20) not null,
    next_review_at date not null,
    review_count int not null,
    note text null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_mistake_question foreign key (question_id) references questions(id),
    constraint fk_mistake_practice_record foreign key (practice_record_id) references practice_records(id),
    constraint fk_mistake_knowledge foreign key (knowledge_point_id) references knowledge_points(id)
);

create table mock_exams (
    id bigint primary key auto_increment,
    name varchar(160) not null,
    type varchar(20) not null,
    duration_minutes int not null,
    total_score decimal(10,2) not null,
    description text null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table mock_exam_questions (
    id bigint primary key auto_increment,
    mock_exam_id bigint not null,
    question_id bigint not null,
    sort_order int not null,
    score decimal(10,2) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_mock_exam_question_exam foreign key (mock_exam_id) references mock_exams(id),
    constraint fk_mock_exam_question_question foreign key (question_id) references questions(id)
);

create table mock_exam_attempts (
    id bigint primary key auto_increment,
    mock_exam_id bigint not null,
    status varchar(20) not null,
    started_at datetime(6) not null,
    submitted_at datetime(6) null,
    objective_score decimal(10,2) not null,
    subjective_score decimal(10,2) not null,
    total_score decimal(10,2) not null,
    duration_seconds int not null,
    self_review_summary text null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_mock_exam_attempt_exam foreign key (mock_exam_id) references mock_exams(id)
);

create table mock_exam_attempt_answers (
    id bigint primary key auto_increment,
    attempt_id bigint not null,
    question_id bigint not null,
    answer_text text null,
    auto_score decimal(10,2) null,
    subjective_score decimal(10,2) null,
    result varchar(20) null,
    feedback text null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_mock_exam_attempt_answer_attempt foreign key (attempt_id) references mock_exam_attempts(id),
    constraint fk_mock_exam_attempt_answer_question foreign key (question_id) references questions(id)
);

create table notes (
    id bigint primary key auto_increment,
    title varchar(160) not null,
    content longtext not null,
    summary varchar(255) null,
    favorite boolean not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table note_links (
    id bigint primary key auto_increment,
    note_id bigint not null,
    link_type varchar(20) not null,
    target_id bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_note_link_note foreign key (note_id) references notes(id)
);
