CREATE TABLE ARBEIDSLISTE (
  AKTOERID                     VARCHAR2(20) NOT NULL,
  SIST_ENDRET_AV_VEILEDERIDENT VARCHAR2(20),
  KOMMENTAR                    VARCHAR2(1000),
  FRIST                        TIMESTAMP,
  ENDRINGSTIDSPUNKT            TIMESTAMP,
  PRIMARY KEY (AKTOERID)
);