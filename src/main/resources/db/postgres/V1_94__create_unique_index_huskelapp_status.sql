CREATE UNIQUE INDEX unique_fnr_active_status
    ON HUSKELAPP (FNR)
    WHERE STATUS = 'AKTIV';