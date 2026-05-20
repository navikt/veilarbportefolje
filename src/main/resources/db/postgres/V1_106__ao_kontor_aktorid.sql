ALTER TABLE ao_kontor ADD COLUMN aktorid varchar(20) NOT NULL;
ALTER TABLE ao_kontor DROP CONSTRAINT ao_kontor_pkey;
ALTER TABLE ao_kontor ADD PRIMARY KEY (aktorid);
ALTER TABLE ao_kontor ADD CONSTRAINT ao_kontor_ident_unique UNIQUE (ident);
