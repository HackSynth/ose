create table ai_provider_settings (
    id bigint primary key auto_increment,
    provider varchar(20) not null,
    enabled boolean not null default false,
    api_key_encrypted longtext null,
    api_key_mask varchar(32) null,
    base_url varchar(255) null,
    default_model varchar(120) null,
    timeout_ms int null,
    max_retries int null,
    temperature double null,
    config_source varchar(20) not null,
    last_health_status varchar(20) null,
    last_health_message varchar(255) null,
    last_health_checked_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint uk_ai_provider_settings_provider unique (provider)
);
