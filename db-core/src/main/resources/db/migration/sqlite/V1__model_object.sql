-- SQLite. Timestamps are INTEGER epoch milliseconds and booleans are 0/1: SQLite has no date or boolean types, and
-- this is how the JDBC driver stores java.sql.Timestamp and boolean values.
create table model_object
(
    id                integer primary key autoincrement,
    name              text    not null,
    description       text    not null,
    model_object_type text    not null,
    deleted           integer not null default 0,
    created           integer not null,
    last_updated      integer not null
);

create index model_object_name on model_object (name);
