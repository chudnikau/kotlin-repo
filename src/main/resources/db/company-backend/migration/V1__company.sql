create table company
(
    code VARCHAR(12) NOT NULL,
    data jsonb NOT NULL,
    created_time TIMESTAMP NOT NULL,
    updated_time TIMESTAMP NULL,
    constraint pk_company primary key (code)
);
