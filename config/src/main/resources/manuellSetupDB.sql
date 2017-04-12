CREATE OR REPLACE PROCEDURE REFRESH_MV AS
  BEGIN
    DBMS_MVIEW.REFRESH('OPPFOLGINGSBRUKER', 'F');
    DBMS_MVIEW.REFRESH('SIKKERHETSTILTAK_TYPE', 'F');
    DBMS_MVIEW.REFRESH('HOVEDMAAL', 'F');
    DBMS_MVIEW.REFRESH('RETTIGHETSGRUPPETYPE', 'F');
    DBMS_MVIEW.REFRESH('KVALIFISERINGSGRUPPETYPE', 'F');
    DBMS_MVIEW.REFRESH('FORMIDLINGSGRUPPETYPE', 'F');
  END REFRESH_MV;
/

CREATE OR REPLACE TRIGGER restartmateralizedviews
AFTER STARTUP ON DATABASE
  BEGIN
    DECLARE
      numberOfJobs number;
    BEGIN
      SELECT COUNT(*) INTO numberOfJobs FROM USER_SCHEDULER_JOBS WHERE JOB_NAME = 'REFRESH_MV_JOB';

      if numberOfJobs = 0 then
        dbms_scheduler.create_schedule(
            schedule_name => 'REFRESH_MV_POLLER',
            start_date => trunc(sysdate)+18/24,
            repeat_interval => 'freq=MINUTELY;interval=1',
            comments => 'Kjorer hvert minutt for a refreshe mview mot arena'
        );
        dbms_scheduler.create_program(
            program_name => 'REFRESH_MV_PROGRAM',
            program_type => 'STORED_PROCEDURE',
            program_action => 'REFRESH_MV',
            enabled => true
        );
        dbms_scheduler.create_job(
            job_name => 'REFRESH_MV_JOB',
            program_name => 'REFRESH_MV_PROGRAM',
            schedule_name => 'REFRESH_MV_POLLER',
            enabled => true,
            AUTO_DROP => FALSE
        );
      end if;
    END;
  END restartmateralizedviews;/
