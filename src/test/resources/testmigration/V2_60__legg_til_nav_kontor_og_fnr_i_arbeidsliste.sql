ALTER TABLE ARBEIDSLISTE
    ADD NAV_KONTOR_FOR_ARBEIDSLISTE VARCHAR2(24);

ALTER TABLE ARBEIDSLISTE
    ADD FNR VARCHAR2(11);

merge into ARBEIDSLISTE arb
using (
    select *
    from VW_PORTEFOLJE_INFO
) vw
on (arb.AKTOERID = vw.AKTOERID)
when matched then
    update
    set arb.FNR                         = vw.FODSELSNR,
        arb.NAV_KONTOR_FOR_ARBEIDSLISTE = vw.NAV_KONTOR;