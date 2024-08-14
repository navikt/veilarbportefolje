CREATE TABLE IF NOT EXISTS arbeidsliste_kopi_2024_01_16 AS TABLE arbeidsliste;
CREATE TABLE IF NOT EXISTS fargekategori_kopi_2024_01_16 AS TABLE fargekategori;

INSERT INTO fargekategori(id, fnr, verdi, sist_endret, sist_endret_av_veilederident)
    (SELECT uuid_in(overlay(overlay(md5(random() :: text || ':' || clock_timestamp() :: text) placing '4' from 13)
                            placing
                            to_hex(floor(random() * (11 - 8 + 1) + 8) :: int) :: text from 17) :: cstring),
            ai.fnr,
            CASE
                WHEN kategori = 'BLA' THEN 'FARGEKATEGORI_A'
                WHEN kategori = 'GRONN' THEN 'FARGEKATEGORI_B'
                WHEN kategori = 'GUL' THEN 'FARGEKATEGORI_C'
                WHEN kategori = 'LILLA' THEN 'FARGEKATEGORI_D'
                END,
            arb.endringstidspunkt,
            arb.sist_endret_av_veilederident
     FROM arbeidsliste arb,
          aktive_identer ai
     WHERE arb.aktoerid = ai.aktorid
       AND arb.kategori IS NOT NULL)
ON CONFLICT (fnr) DO NOTHING;

UPDATE arbeidsliste SET kategori = null WHERE kategori IS NOT NULL;