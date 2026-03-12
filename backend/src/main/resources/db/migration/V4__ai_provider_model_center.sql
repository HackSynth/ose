create table ai_providers (
    id varchar(64) primary key,
    provider_type varchar(32) not null,
    display_name varchar(120) not null,
    enabled boolean not null default false,
    key_rotation_strategy varchar(40) not null,
    base_url varchar(255) null,
    base_url_mode varchar(20) not null,
    default_model varchar(120) null,
    timeout_ms int null,
    max_retries int null,
    temperature double null,
    remark varchar(500) null,
    config_source varchar(20) not null,
    health_status varchar(20) null,
    last_checked_at datetime(6) null,
    health_message varchar(255) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

create table ai_provider_api_keys (
    id varchar(64) primary key,
    provider_id varchar(64) not null,
    key_encrypted longtext not null,
    key_mask varchar(32) not null,
    enabled boolean not null default true,
    sort_order int not null default 0,
    consecutive_failures int not null default 0,
    last_used_at datetime(6) null,
    last_failed_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_ai_provider_api_keys_provider foreign key (provider_id) references ai_providers(id)
);

create table ai_models (
    id varchar(64) primary key,
    provider_id varchar(64) not null,
    model_id varchar(120) not null,
    display_name varchar(120) not null,
    model_type varchar(20) not null,
    capability_tags varchar(255) null,
    enabled boolean not null default true,
    sort_order int not null default 0,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint fk_ai_models_provider foreign key (provider_id) references ai_providers(id)
);

create table ai_default_model_settings (
    id varchar(32) primary key,
    question_generation_provider_id varchar(64) null,
    question_generation_model_id varchar(64) null,
    review_summary_provider_id varchar(64) null,
    review_summary_model_id varchar(64) null,
    practice_recommendation_provider_id varchar(64) null,
    practice_recommendation_model_id varchar(64) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null
);

insert into ai_default_model_settings (
    id,
    created_at,
    updated_at
) values (
    'DEFAULT',
    current_timestamp(),
    current_timestamp()
);

insert into ai_providers (
    id,
    provider_type,
    display_name,
    enabled,
    key_rotation_strategy,
    base_url,
    base_url_mode,
    default_model,
    timeout_ms,
    max_retries,
    temperature,
    remark,
    config_source,
    health_status,
    last_checked_at,
    health_message,
    created_at,
    updated_at
)
select
    concat('legacy-', lower(provider)),
    provider,
    case provider
        when 'OPENAI' then 'OpenAI'
        when 'ANTHROPIC' then 'Anthropic'
        else provider
    end,
    enabled,
    'SEQUENTIAL_ROUND_ROBIN',
    base_url,
    'ROOT',
    default_model,
    timeout_ms,
    max_retries,
    temperature,
    '由旧版 ai_provider_settings 自动迁移',
    case
        when config_source = 'DB' then 'DB'
        else 'HYBRID'
    end,
    last_health_status,
    last_health_checked_at,
    last_health_message,
    created_at,
    updated_at
from ai_provider_settings;

insert into ai_provider_api_keys (
    id,
    provider_id,
    key_encrypted,
    key_mask,
    enabled,
    sort_order,
    consecutive_failures,
    created_at,
    updated_at
)
select
    concat('legacy-key-', lower(provider)),
    concat('legacy-', lower(provider)),
    api_key_encrypted,
    api_key_mask,
    true,
    0,
    0,
    created_at,
    updated_at
from ai_provider_settings
where api_key_encrypted is not null and api_key_encrypted <> '';

insert into ai_models (
    id,
    provider_id,
    model_id,
    display_name,
    model_type,
    capability_tags,
    enabled,
    sort_order,
    created_at,
    updated_at
)
select
    concat('legacy-model-', lower(provider)),
    concat('legacy-', lower(provider)),
    default_model,
    default_model,
    'CHAT',
    'legacy',
    true,
    0,
    created_at,
    updated_at
from ai_provider_settings
where default_model is not null and default_model <> '';
