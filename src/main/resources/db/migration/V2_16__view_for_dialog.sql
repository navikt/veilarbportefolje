CREATE VIEW VW_DIALOG AS (
SELECT 
  BD.AKTOERID AS AKTOERID,
  NULL AS OPPDATERT, 
  BD.VENTERPASVARFRANAV AS VENTER_PA_NAV, 
  BD.VENTERPASVARFRABRUKER AS VENTER_PA_BRUKER
FROM BRUKER_DATA BD
WHERE BD.AKTOERID IS NOT NULL);

-- TO REMOVE CHANGES:
--
-- DROP VIEW VW_DIALOG;
