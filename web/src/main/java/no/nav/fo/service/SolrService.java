package no.nav.fo.service;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Bruker;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.desc;
import static org.slf4j.LoggerFactory.getLogger;

public class SolrService {

    private static final Logger logger = getLogger(SolrService.class);

    @Inject
    private HttpSolrServer server;

    @Inject
    private BrukerRepository brukerRepository;

    @Scheduled(cron = "${veilarbportefolje.cron.hovedindeksering}")
    public void hovedindeksering() {
        if (isSlaveNode()) {
            logger.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        logger.info("Starter hovedindeksering");

        List<Map<String, Object>> rader = brukerRepository.retrieveAlleBrukere();
        List<SolrInputDocument> dokumenter = rader.stream().map(this::mapRadTilDokument).collect(Collectors.toList());

        deleteAllDocuments();
        addDocuments(dokumenter);
        updateTimestamp(rader);

        logger.info("Hovedindeksering fullført!");
        logger.info(dokumenter.size() + " dokumenter ble lagt til i solrindeksen");
    }

    @Scheduled(cron = "${veilarbportefolje.cron.deltaindeksering}")
    public void deltaindeksering() {
        if (isSlaveNode()) {
            logger.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        logger.info("Starter deltaindeksering");
        List<Map<String, Object>> rader = brukerRepository.retrieveOppdaterteBrukere();
        if (rader.isEmpty()) {
            logger.info("Ingen nye dokumenter i databasen");
            return;
        }

        List<SolrInputDocument> dokumenter = rader.stream().map(this::mapRadTilDokument).collect(Collectors.toList());
        addDocuments(dokumenter);
        updateTimestamp(rader);
        logger.info("Deltaindeksering fullført!");
        logger.info(dokumenter.size() + " dokumenter ble oppdatert/lagt til i solrindeksen");
    }

    public List<Bruker> hentBrukere(String enhetId, String sortOrder) {
        SolrQuery.ORDER order = SolrQuery.ORDER.asc;
        if ("descending".equals(sortOrder)) {
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
            brukere = results.stream().map(Bruker::new).collect(toList());
        } catch (SolrServerException e) {
            logger.error("Spørring mot indeks feilet: ", e.getMessage(), e);
        }
        return brukere;
    }

    Map<String, Object> nyesteBruker(List<Map<String, Object>> brukere) {
        return brukere.stream().max(Comparator.comparing(r -> new DateTime(r.get("tidsstempel")).getMillis())).get();
    }

    String parseDato(Object dato) {
        if (dato == null) {
            return null;
        } else if (dato.equals("TZ")) {
            return null;
        } else {
            return dato.toString();
        }
    }

    static boolean isSlaveNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return !Boolean.parseBoolean(isMasterString);
    }

    private void updateTimestamp(List<Map<String, Object>> rader) {
        Timestamp tidsstempel = (Timestamp) nyesteBruker(rader).get("tidsstempel");
        brukerRepository.updateTidsstempel(tidsstempel);
    }

    private UpdateResponse addDocuments(List<SolrInputDocument> dokumenter) {
        UpdateResponse response = null;
        try {
            response = server.add(dokumenter);
            checkSolrUpdateResponseCode(response);
        } catch (SolrServerException | IOException e) {
            logger.error("Kunne ikke legge til dokumenter.", e.getMessage(), e);
        } catch (SolrUpdateResponseCodeException e) {
            logger.error(e.getMessage());
        }
        return response;
    }

    private UpdateResponse deleteAllDocuments() {
        UpdateResponse response = null;
        try {
            response = server.deleteByQuery("*:*");
            checkSolrUpdateResponseCode(response);
        } catch (SolrServerException | IOException e) {
            logger.error("Kunne ikke slette dokumenter.", e.getMessage(), e);
        } catch (SolrUpdateResponseCodeException e) {
            logger.error(e.getMessage());
        }
        return response;
    }

    private void checkSolrUpdateResponseCode(UpdateResponse response) {
        if (response.getStatus() != 0) {
            throw new SolrUpdateResponseCodeException(String.format("Solr returnerte med responskode %s", response.getStatus()));
        }
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
        document.addField("veileder_id", null);
        return document;
    }
}
