CREATE TABLE BRUKER_IDENTER
(
    PERSON    VARCHAR(25) NOT NULL,
    IDENT     VARCHAR(30) NOT NULL,
    HISTORISK BOOLEAN     NOT NULL,
    GRUPPE    VARCHAR(30) NOT NULL,

    PRIMARY KEY (IDENT)
);

CREATE SEQUENCE PDL_PERSON_SEQ AS BIGINT START 1;