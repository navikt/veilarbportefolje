alter table bruker_identer
    alter column ident type varchar(20) using ident::varchar(20);

