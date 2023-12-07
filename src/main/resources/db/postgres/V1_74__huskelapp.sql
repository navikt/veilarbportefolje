CREATE TABLE Huskelapp
(
    endrings_id           uuid primary key,
    huskelapp_id          uuid,
    fnr                   varchar(11) not null,
    enhet_id              varchar(4)  not null,
    endret_av_veileder    varchar(10) not null,
    endret_dato           timestamp            default now(),
    frist                 timestamp,
    kommentar             varchar(200),
    status                varchar(10)
);

create index huskelappid on Huskelapp (huskelapp_id);
create index huskelappfnr on Huskelapp (fnr);
create index huskelappenhetid on Huskelapp (enhet_id);
