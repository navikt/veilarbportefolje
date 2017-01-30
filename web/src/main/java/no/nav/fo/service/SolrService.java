package no.nav.fo.service;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Bruker;
import no.nav.virksomhet.organisering.enhet.v1.Enhet;
import no.nav.virksomhet.organisering.enhet.v1.EnhetUtvidelse1;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.desc;
import static org.slf4j.LoggerFactory.getLogger;

public class SolrService {

    private static final Logger logger = getLogger(SolrService.class);

    @Inject
    HttpSolrServer server;

    @Inject
    BrukerRepository brukerRepository;

    @Scheduled(cron = "${veilarbportefolje.cron.hovedindeksering}")
    public void hovedindeksering() {
        if (isSlaveNode()) {
            logger.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        logger.info("Starter hovedindeksering");
        try {
            deleteAllDocuments();
            addAllDocuments();
            server.commit();
        } catch (SolrServerException | IOException e) {
            logger.error("Hovedindeksering feilet. Kunne ikke utføre commit. ", e.getMessage(), e);
        }
        logger.info("Hovedindeksering fullført!");
    }

    public List<Bruker> hentBrukere(String enhetId, String sortOrder) {
        SolrQuery.ORDER order = SolrQuery.ORDER.asc;
        if (sortOrder.equals("descending")) {
            order = desc;
        }

        String queryString = "nav_kontor: " + enhetId;
        SolrQuery solrQuery = new SolrQuery(queryString);
        solrQuery.addSort("etternavn", order);
        solrQuery.addSort("fornavn", order);

        List<Bruker> brukere = new ArrayList<>();
        try {
            QueryResponse response = server.query(solrQuery);
            SolrDocumentList results = response.getResults();
            logger.debug(results.toString());
        } catch (SolrServerException e) {
            logger.error("Spørring mot indeks feilet: ", e.getMessage(), e);
        }
        return brukere;
    }

    private void addAllDocuments() {
        List<Map<String, Object>> rader = brukerRepository.retrieveAlleBrukere();
        List<SolrInputDocument> dokumenter = rader.stream().map(this::mapRadTilDokument).collect(Collectors.toList());
        try {
            server.add(dokumenter);
        } catch (SolrServerException | IOException e) {
            logger.error("Kunne ikke legge til dokumenter i indeks: ", e.getMessage(), e);
        }
    }

    private void deleteAllDocuments() {
        try {
            server.deleteByQuery("*:*");
        } catch (SolrServerException | IOException e) {
            logger.error("Sletting av dokumenter i indeksen feilet: ", e.getMessage(), e);
        }
    }

    static boolean isSlaveNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return !BooleanUtils.toBoolean(isMasterString);
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
        if (dato == null) {
            return null;
        } else if (dato.equals("TZ")) {
            return null;
        } else {
            return dato.toString();
        }
    }
}
