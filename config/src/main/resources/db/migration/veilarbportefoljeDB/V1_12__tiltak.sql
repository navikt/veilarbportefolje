CREATE TABLE TILTAKKODEVERK (
  KODE VARCHAR(20) NOT NULL,
  VERDI VARCHAR(255) NOT NULL,
  PRIMARY KEY (KODE)
)

CREATE TABLE BRUKERTILTAK (
  AKTOERID VARCHAR(20) NOT NULL,
  PERSONID VARCHAR(20),
  TILTAKSKODE VARCHAR(20) NOT NULL,
  FOREIGN KEY (TILTAKSKODE) REFERENCES TILTAKKODEVERK(KODE)
)

CREATE TABLE ENHETTILTAK (
  ENHETID VARCHAR(20) NOT NULL,
  TILTAKSKODE VARCHAR(20) NOT NULL,
  FOREIGN KEY (TILTAKSKODE) REFERENCES TILTAKKODEVERK(KODE)
)
