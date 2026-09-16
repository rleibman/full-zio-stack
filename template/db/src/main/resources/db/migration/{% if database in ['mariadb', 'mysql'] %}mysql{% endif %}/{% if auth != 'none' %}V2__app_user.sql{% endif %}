-- MariaDB and MySQL. Only in projects generated with authentication.
-- `created` is epoch milliseconds rather than a datetime: UserStore is one plain-JDBC implementation shared by every
-- database, so it stores the column the same way everywhere (see UserStore.scala).
create table app_user
(
    id            int          not null auto_increment primary key,
    email         varchar(255) not null,
    name          varchar(255) not null,
    active        boolean      not null default false,
    created       bigint       not null,
    deleted       boolean      not null default false,
    password_hash varchar(255) null
);

create unique index app_user_email on app_user (email);
