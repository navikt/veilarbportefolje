CREATE TABLE Huskelapp
(
    ID                    uuid primary key,
    fnr                   varchar(11) not null,
    enhet_id              varchar(4)  not null,
    opprettet_av_veileder varchar(10) not null,
    opprettet_dato        timestamp            default now(),
    frist                 timestamp,
    kommentar             varchar(200),
    status                int         not null default 0,
    arkivert_dato         timestamp
);

create index huskelappfnr on Huskelapp (fnr);
create index huskelappenhetid on Huskelapp (enhet_id);