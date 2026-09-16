-- SQLite. Only in projects generated with authentication. Booleans are 0/1 and `created` is epoch milliseconds, which
-- is what UserStore (one plain-JDBC implementation shared by every database) writes everywhere.
create table app_user
(
    id            integer primary key autoincrement,
    email         text    not null,
    name          text    not null,
    active        integer not null default 0,
    created       integer not null,
    deleted       integer not null default 0,
    password_hash text    null
);

create unique index app_user_email on app_user (email);
