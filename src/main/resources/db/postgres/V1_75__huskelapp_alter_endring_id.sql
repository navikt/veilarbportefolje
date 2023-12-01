alter table huskelapp
    drop CONSTRAINT huskelapp_pkey;
alter table huskelapp
    drop column endrings_id;
alter table huskelapp
    add column endrings_id serial primary key;