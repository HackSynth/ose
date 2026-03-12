alter table questions
    add column ai_generated boolean not null default false,
    add column ai_provider varchar(20) null,
    add column ai_model varchar(120) null;

create table ai_generation_records (
    id bigint primary key auto_increment,
    provider varchar(20) not null,
    model varchar(120) not null,
    question_type varchar(32) not null,
    topic_type varchar(32) not null,
    difficulty varchar(32) not null,
    requested_count int not null,
    success_count int not null,
    status varchar(32) not null,
    request_payload text null,
    response_payload longtext null,
    prompt_hash varchar(64) null,
    prompt_summary text null,
    error_message text null,
    finish_status varchar(32) null,
    usage_input_tokens int null,
    usage_output_tokens int null,
    estimated_cost decimal(10,4) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);
