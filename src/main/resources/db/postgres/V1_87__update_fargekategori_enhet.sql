UPDATE fargekategori
SET enhet_id = (SELECT nav_kontor
                FROM oppfolgingsbruker_arena_v2
                WHERE fargekategori.fnr = oppfolgingsbruker_arena_v2.fodselsnr)
WHERE enhet_id is null;