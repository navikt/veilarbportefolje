CREATE TYPE KAFKA_MESSAGE_TYPE AS enum ('PRODUCED','CONSUMED');

CREATE TABLE FEILET_KAFKA_MELDING
(
    ID             BIGINT                              NOT NULL PRIMARY KEY,
    TOPIC          VARCHAR(100)                        NOT NULL,
    KEY            VARCHAR(40)                         NOT NULL,
    PAYLOAD        JSON                                NOT NULL,
    MESSAGE_TYPE   kafka_message_type                  NOT NULL,
    MESSAGE_OFFSET BIGINT,
    CREATED_AT     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE KAFKA_CONSUMER_RECORD
(
    ID               BIGINT                              NOT NULL,
    TOPIC            VARCHAR(100)                        NOT NULL,
    PARTITION        INTEGER                             NOT NULL,
    RECORD_OFFSET    BIGINT                              NOT NULL,
    RETRIES          INTEGER   DEFAULT 0                 NOT NULL,
    LAST_RETRY       TIMESTAMP(6) WITHOUT TIME ZONE,
    KEY              BYTEA,
    VALUE            BYTEA,
    HEADERS_JSON     TEXT,
    RECORD_TIMESTAMP BIGINT,
    CREATED_AT       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (TOPIC, PARTITION, RECORD_OFFSET)
);
