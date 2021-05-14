CREATE SEQUENCE feilet_kafka_melding_serial AS BIGINT START 1;

ALTER TABLE FEILET_KAFKA_MELDING
    ALTER COLUMN ID SET DEFAULT nextval('feilet_kafka_melding_serial');

CREATE SEQUENCE kafka_consumer_record_serial AS BIGINT START 1;

ALTER TABLE KAFKA_CONSUMER_RECORD
    ALTER COLUMN ID SET DEFAULT nextval('kafka_consumer_record_serial');