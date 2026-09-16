-- MariaDB and MySQL. Timestamps have millisecond precision and are stored in the server JVM's time zone (JDBC
-- converts Instant through java.sql.Timestamp), so run every server in the same zone; UTC is recommended.
create table model_object
(
    id                int          not null auto_increment primary key,
    name              varchar(255) not null,
    description       text         not null,
    model_object_type varchar(32)  not null,
    deleted           boolean      not null default false,
    created           datetime(3)  not null,
    last_updated      datetime(3)  not null
);

create index model_object_name on model_object (name);
