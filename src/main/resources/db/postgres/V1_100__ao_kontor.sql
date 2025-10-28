-- ao = Arbeidsoppfølging
CREATE TABLE ao_kontor (
    ident varchar(11) primary key,
    kontor_id varchar(4),
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp
);
