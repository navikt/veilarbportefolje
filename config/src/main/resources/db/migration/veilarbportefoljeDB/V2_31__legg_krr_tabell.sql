create table KRR
(
  FODSELSNR VARCHAR2(11) not null
    constraint KRR_FODSELSNR_PK
    primary key,
  RESERVASJON VARCHAR2(1),
  SISTEVERIFISERT TIMESTAMP(6),
  LAGTTILIDB TIMESTAMP(6)
)