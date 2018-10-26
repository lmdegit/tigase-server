-- QUERY START:
create table tig_stats_log (
  lid            serial,
  ts             TIMESTAMP                 DEFAULT CURRENT_TIMESTAMP,
  hostname       varchar(2049)    NOT NULL,
  cpu_usage      double precision not null default 0,
  mem_usage      double precision not null default 0,
  uptime         bigint           not null default 0,
  vhosts         int              not null default 0,
  sm_packets     bigint           not null default 0,
  muc_packets    bigint           not null default 0,
  pubsub_packets bigint           not null default 0,
  c2s_packets    bigint           not null default 0,
  s2s_packets    bigint           not null default 0,
  ext_packets    bigint           not null default 0,
  presences      bigint           not null default 0,
  messages       bigint           not null default 0,
  iqs            bigint           not null default 0,
  registered     bigint           not null default 0,
  c2s_conns      int              not null default 0,
  s2s_conns      int              not null default 0,
  bosh_conns     int              not null default 0,
  primary key (ts, hostname(255))
);
-- QUERY END:

-- QUERY START:
do $$
begin
    if not exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_stats_log' and column_name = 'ws2s_conns') then
       ALTER TABLE tig_stats_log ADD `ws2s_conns` INT not null default 0;
    end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
    if not exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_stats_log' and column_name = 'ws2s_packets') then
       ALTER TABLE tig_stats_log ADD `ws2s_packets` INT not null default 0;
    end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
    if not exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_stats_log' and column_name = 'sm_sessions') then
       ALTER TABLE tig_stats_log ADD `sm_sessions` INT not null default 0;
    end if;
end$$;
-- QUERY END:

-- QUERY START:
do $$
begin
    if not exists (select 1 from information_schema.columns where table_catalog = current_database() and table_schema = 'public' and table_name = 'tig_stats_log' and column_name = 'sm_connections') then
       ALTER TABLE tig_stats_log ADD `sm_connections` INT not null default 0;
    end if;
end$$;
-- QUERY END:
