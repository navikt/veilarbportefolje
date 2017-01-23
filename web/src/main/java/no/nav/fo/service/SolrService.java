package no.nav.fo.service;

import no.nav.fo.database.BrukerRepository;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class SolrService {

    private static final Logger logger =  getLogger(SolrService.class);

    @Inject
    HttpSolrServer server;

    @Inject
    BrukerRepository brukerRepository;

    @Scheduled(cron = "${veilarbportefolje.cron.hovedindeksering}")
    public void hovedindeksering() {
        List<Map<String, Object>> rader = brukerRepository.retrieveAlleBrukere();
        List<SolrInputDocument> dokumenter = rader.stream().map(rad -> mapRadTilDokument(rad)).collect(Collectors.toList());
        try {
            server.add(dokumenter);
            server.commit();
        } catch (SolrServerException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Hovedindeksering fullført!");
    }

    @Scheduled(cron = "${veilarbportefolje.cron.deltaindeksering}")
    public void deltaindeksering() {
        List<Map<String, Object>> rader = brukerRepository.retrieveNyeBrukere();
        List<SolrInputDocument> dokumenter = rader.stream().map(rad -> mapRadTilDokument(rad)).collect(Collectors.toList());
        try {
            server.add(dokumenter);
            server.commit();
        } catch (SolrServerException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Deltaindeksering fullført!");
    }

    private SolrInputDocument mapRadTilDokument(Map<String, Object> rad) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField("person_id", rad.get("person_id").toString());
        document.addField("fodselsnr", rad.get("fodselsnr").toString());
        document.addField("fornavn", rad.get("fornavn").toString());
        document.addField("etternavn", rad.get("etternavn").toString());
        document.addField("nav_kontor", rad.get("nav_kontor").toString());
        document.addField("formidlingsgruppekode", rad.get("formidlingsgruppekode").toString());
        document.addField("iserv_fra_dato", parseDato(rad.get("iserv_fra_dato")));
        document.addField("kvalifiseringsgruppekode", rad.get("kvalifiseringsgruppekode").toString());
        document.addField("rettighetsgruppekode", rad.get("rettighetsgruppekode").toString());
        document.addField("hovedmaalkode", rad.get("hovedmaalkode") != null ? rad.get("hovedmaalkode").toString() : null);
        document.addField("sikkerhetstiltak_type_kode", rad.get("sikkerhetstiltak_type_kode") != null ? rad.get("sikkerhetstiltak_type_kode").toString() : null);
        document.addField("fr_kode", rad.get("fr_kode") != null ? rad.get("fr_kode").toString() : null);
        document.addField("sperret_ansatt", rad.get("sperret_ansatt").toString());
        document.addField("er_doed", rad.get("er_doed").toString());
        document.addField("doed_fra_dato", parseDato(rad.get("doed_fra_dato")));
        return document;
    }

    private String parseDato(Object dato) {
        if(dato == null) {
            return null;
        }
        else if(dato.equals("TZ")) {
            return null;
        }
        else {
            return dato.toString();
        }
    }
}
