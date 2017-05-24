-- Lag synke-prosedyre
CREATE OR REPLACE PROCEDURE synk_med_arena IS
BEGIN
  dbms_mview.refresh('HOVEDMAAL','?');
  dbms_mview.refresh('FORMIDLINGSGRUPPETYPE','?');
  dbms_mview.refresh('KVALIFISERINGSGRUPPETYPE','?');
  dbms_mview.refresh('RETTIGHETSGRUPPETYPE','?');
  dbms_mview.refresh('SIKKERHETSTILTAK_TYPE','?');
  dbms_mview.refresh('OPPFOLGINGSBRUKER','?');
END;
/

-- Lag en scheduler-job som synker
BEGIN
  dbms_scheduler.create_job(
    job_name        => 'ARENA_SYNK',
    job_type        => 'STORED_PROCEDURE',
    job_action      => 'SYNK_MED_ARENA',
    start_date      => SYSDATE + 1/1440,
    repeat_interval => 'FREQ=MINUTELY;INTERVAL=1',
    enabled         => TRUE,
    comments        => 'Synker med Arena hvert minutt'
  );
END;
/