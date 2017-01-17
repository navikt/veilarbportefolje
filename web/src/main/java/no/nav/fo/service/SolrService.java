package no.nav.fo.service;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class SolrService {

    private static final Logger logger =  getLogger(SolrService.class);

    @Inject
    HttpSolrServer server;

    public void leggTilDokumenter() {
        SolrInputDocument document1 = new SolrInputDocument();
        document1.addField("fornavn", "Ola");
        document1.addField("etternavn", "Normann");
        document1.addField("fnr", "***REMOVED***");

        SolrInputDocument document2 = new SolrInputDocument();
        document2.addField("fornavn", "Edward");
        document2.addField("etternavn", "Grieg");
        document2.addField("fnr", "222222222");

        SolrInputDocument document3 = new SolrInputDocument();
        document3.addField("fornavn", "Ole");
        document3.addField("etternavn", "Bull");
        document3.addField("fnr", "111111111");

        List<SolrInputDocument> documents = new ArrayList();
        documents.add(document1);
        documents.add(document2);
        documents.add(document3);

        try {
            server.add(documents);
            server.commit();
        } catch (SolrServerException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        logger.info("Solrindeks har blitt oppdatert");
    }

    public void slettAlleDokumenter() {
        try {
            server.deleteByQuery("*:*");
            server.commit();
        } catch (SolrServerException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Solrindeks har blitt slettet");
    }

//    @Scheduled(fixedDelay = 5000)
    private void scheduledFullOppdatering() {
        slettAlleDokumenter();
        leggTilDokumenter();
        System.out.println("Scheduledtask completed");
    }

}
