package no.nav.fo.service;

import javaslang.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.Bruker;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        List<SolrInputDocument> dokumenter = brukerRepository.retrieveAlleBrukere();
        deleteAllDocuments();
        addDocuments(dokumenter);
        commit();
        brukerRepository.updateTidsstempel(timestamp);

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
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());

        List<SolrInputDocument> dokumenter = brukerRepository.retrieveOppdaterteBrukere();
        if (dokumenter.isEmpty()) {
            logger.info("Ingen nye dokumenter i databasen");
            return;
        }

        addDocuments(dokumenter);
        brukerRepository.updateTidsstempel(timestamp);
        commit();

        logger.info("Deltaindeksering fullført!");
        logger.info(dokumenter.size() + " dokumenter ble oppdatert/lagt til i solrindeksen");
    }

    public List<Bruker> hentBrukere(String enhetId, String sortOrder) {
        List<Bruker> brukere = new ArrayList<>();
        try {
            QueryResponse response = server.query(buildSolrQuery(enhetId, sortOrder));
            SolrDocumentList results = response.getResults();
            logger.debug(results.toString());
            brukere = results.stream().map(Bruker::of).collect(toList());
        } catch (SolrServerException e) {
            logger.error("Spørring mot indeks feilet: ", e.getMessage(), e);
        }
        return brukere;
    }

    SolrQuery buildSolrQuery(String enhetId, String sortOrder) {
        SolrQuery.ORDER order = SolrQuery.ORDER.asc;
        if ("descending".equals(sortOrder)) {
            order = desc;
        }
        String queryString = "enhet_id: " + enhetId;
        SolrQuery solrQuery = new SolrQuery(queryString);
        solrQuery.addSort("etternavn", order);
        solrQuery.addSort("fornavn", order);
        return solrQuery;
    }

    static boolean isSlaveNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return !Boolean.parseBoolean(isMasterString);
    }

    private Try<UpdateResponse> commit() {
        return Try.of(() -> server.commit())
                .onFailure(e -> logger.error("Kunne ikke gjennomføre commit ved indeksering!", e));
    }

    private List<SolrInputDocument> addDocuments(List<SolrInputDocument> dokumenter) {
        // javaslang.collection-API brukes her pga sliding-metoden
        javaslang.collection.List.ofAll(dokumenter)
                .sliding(10000, 10000)
                .forEach(docs -> {
                    try {
                        server.add(docs.toJavaList());
                        logger.info(String.format("Legger til %d dokumenter i indeksen", docs.length()));
                    } catch (SolrServerException | IOException e) {
                        logger.error("Kunne ikke legge til dokumenter.", e.getMessage(), e);
                    }
                });
        return dokumenter;
    }

    private void deleteAllDocuments() {
        try {
            UpdateResponse response = server.deleteByQuery("*:*");
            checkSolrResponseCode(response.getStatus());
        } catch (SolrServerException | IOException e) {
            logger.error("Kunne ikke slette dokumenter.", e.getMessage(), e);
        } catch (SolrUpdateResponseCodeException e) {
            logger.error(e.getMessage());
        }
    }

    void checkSolrResponseCode(int statusCode) {
        if (statusCode != 0) {
            throw new SolrUpdateResponseCodeException(String.format("Solr returnerte med statuskode %s", statusCode));
        }
    }

}
