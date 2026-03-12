alter table ai_generation_records
    add column provider_id varchar(64) null after id,
    add column provider_display_name varchar(120) null after provider;
