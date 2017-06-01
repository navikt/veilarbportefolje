package no.nav.fo.service;

import javaslang.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import no.nav.fo.util.BatchConsumer;
import no.nav.fo.util.DbUtils;
import no.nav.fo.util.SolrUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.AktivitetUtils.applyAktivitetStatuser;
import static no.nav.fo.util.BatchConsumer.batchConsumer;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class SolrService {

    private static final Logger logger = getLogger(SolrService.class);

    private static final String HOVEDINDEKSERING = "Hovedindeksering";
    private static final String DELTAINDEKSERING = "Deltaindeksering";

    private SolrClient solrClientSlave;
    private SolrClient solrClientMaster;
    private BrukerRepository brukerRepository;

    @Inject
    public SolrService(
            @Named("solrClientMaster") SolrClient solrClientMaster,
            @Named("solrClientSlave") SolrClient solrClientSlave,
            BrukerRepository brukerRepository
    ) {
        this.solrClientMaster = solrClientMaster;
        this.solrClientSlave = solrClientSlave;
        this.brukerRepository = brukerRepository;
    }

    @Transactional
    public void hovedindeksering() {
        if (SolrUtils.isSlaveNode()) {
            logger.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        logger.info("Starter hovedindeksering");
        LocalDateTime t0 = LocalDateTime.now();

        final int[] antallBrukere = {0};
        deleteAllDocuments();

        BatchConsumer<SolrInputDocument> consumer = batchConsumer(10000, (brukere) -> {
            antallBrukere[0] += brukere.size();
            addDocuments(brukere);
            commit();
        });
        brukerRepository.prosesserBrukere(BrukerRepository::erOppfolgingsBruker, consumer);
        consumer.flush(); // Må kalles slik at batcher mindre enn `size` også blir prosessert.

        brukerRepository.updateTidsstempel(Timestamp.valueOf(t0));

        logFerdig(t0, antallBrukere[0], HOVEDINDEKSERING);
    }


    @Scheduled(cron = "${veilarbportefolje.cron.deltaindeksering}")
    @Transactional
    public void deltaindeksering() {
        if (SolrUtils.isSlaveNode()) {
            logger.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        logger.info("Starter deltaindeksering");
        LocalDateTime t0 = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(t0);

        List<SolrInputDocument> dokumenter = brukerRepository.retrieveOppdaterteBrukere();
        if (dokumenter.isEmpty()) {
            logger.info("Ingen nye dokumenter i databasen");
            return;
        }

        addDocuments(dokumenter);
        brukerRepository.updateTidsstempel(timestamp);
        commit();

        logFerdig(t0, dokumenter.size(), DELTAINDEKSERING);
    }

    public List<Bruker> hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        String queryString = byggQueryString(enhetId, veilederIdent);
        return hentBrukere(queryString, sortOrder, sortField, filtervalg);
    }

    String byggQueryString(String enhetId, Optional<String> veilederIdent) {
        return veilederIdent
                .map((ident) -> isBlank(ident) ? null : ident)
                .map((ident) -> "veileder_id: " + ident + " AND enhet_id: " + enhetId)
                .orElse("enhet_id: " + enhetId);
    }

    private List<Bruker> hentBrukere(String queryString, String sortOrder, String sortField, Filtervalg filtervalg) {
        List<Bruker> brukere = new ArrayList<>();
        try {
            QueryResponse response = solrClientSlave.query(SolrUtils.buildSolrQuery(queryString, filtervalg));
            SolrUtils.checkSolrResponseCode(response.getStatus());
            SolrDocumentList results = response.getResults();
            logger.debug(results.toString());
            brukere = results.stream().map(Bruker::of).collect(toList());
        } catch (SolrServerException | IOException e) {
            logger.error("Spørring mot indeks feilet: ", e.getMessage(), e);
        }
        return SolrUtils.sortBrukere(brukere, sortOrder, sortField);
    }

    public void test() {
    }

    public FacetResults hentPortefoljestorrelser(String enhetId) {

        String facetFieldString = "veileder_id";

        SolrQuery solrQuery = SolrUtils.buildSolrFacetQuery("enhet_id: " + enhetId, facetFieldString);

        QueryResponse response = new QueryResponse();
        try {
            response = solrClientSlave.query(solrQuery);
            logger.debug(response.toString());
        } catch (SolrServerException | IOException e) {
            logger.error("Spørring mot indeks feilet", e.getMessage(), e);
        }

        FacetField facetField = response.getFacetField(facetFieldString);

        return SolrUtils.mapFacetResults(facetField);
    }

    public void indekserBrukerdata(String personId) {
        logger.info("Legger bruker med personId % til i indeks ", personId);
        List<Map<String, Object>> rader = brukerRepository.retrieveBrukermedBrukerdata(personId);
        List<SolrInputDocument> dokumenter = rader.stream().map(DbUtils::mapRadTilDokument).collect(Collectors.toList());

        applyAktivitetStatuser(dokumenter, brukerRepository, AktivitetData.aktivitettyperSet);

        addDocuments(dokumenter);
        commit();
        logger.info("Bruker med personId {} lagt til i indeksen", personId);
    }

    private Try<UpdateResponse> commit() {
        return Try.of(() -> solrClientMaster.commit())
                .onFailure(e -> logger.error("Kunne ikke gjennomføre commit ved indeksering!", e));
    }

    private List<SolrInputDocument> addDocuments(List<SolrInputDocument> dokumenter) {
        // javaslang.collection-API brukes her pga sliding-metoden
        javaslang.collection.List.ofAll(dokumenter)
                .sliding(10000, 10000)
                .forEach(docs -> {
                    try {
                        solrClientMaster.add(docs.toJavaList());
                        logger.info(format("Legger til %d dokumenter i indeksen", docs.length()));
                    } catch (SolrServerException | IOException e) {
                        logger.error("Kunne ikke legge til dokumenter.", e.getMessage(), e);
                    }
                });
        return dokumenter;
    }

    private void deleteAllDocuments() {
        try {
            UpdateResponse response = solrClientMaster.deleteByQuery("*:*");
            SolrUtils.checkSolrResponseCode(response.getStatus());
        } catch (SolrServerException | IOException e) {
            logger.error("Kunne ikke slette dokumenter.", e.getMessage(), e);
        } catch (SolrUpdateResponseCodeException e) {
            logger.error(e.getMessage());
        }
    }

    private void logFerdig(LocalDateTime t0, int antall, String indekseringstype) {
        Duration duration = Duration.between(t0, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds();
        String logString = format("%s fullført! | Tid brukt(hh:mm:ss): %02d:%02d:%02d | Dokumenter oppdatert: %d", indekseringstype, hours, minutes, seconds, antall);
        logger.info(logString);

        Event event = MetricsFactory.createEvent("deltaindeksering.fullfort");
        event.addFieldToReport("antall.oppdateringer", antall);
        event.report();
    }

    public StatusTall hentStatusTallForPortefolje(String enhet) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        String nyeBrukere = "-veileder_id:*";
        String inaktiveBrukere = "formidlingsgruppekode:ISERV AND veileder_id:*";
        String venterPaSvarFraNAV = "venterpasvarfranav:*";
        String venterPaSvarFraBruker = "venterpasvarfrabruker:*";

        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFacetQuery(nyeBrukere);
        solrQuery.addFacetQuery(inaktiveBrukere);
        solrQuery.addFacetQuery(venterPaSvarFraNAV);
        solrQuery.addFacetQuery(venterPaSvarFraBruker);
        solrQuery.setRows(0);

        StatusTall statusTall = new StatusTall();
        QueryResponse response;
        try {
            response = solrClientSlave.query(solrQuery);
            long antallTotalt = response.getResults().getNumFound();
            long antallNyeBrukere = response.getFacetQuery().get(nyeBrukere);
            long antallInaktiveBrukere = response.getFacetQuery().get(inaktiveBrukere);
            long antallVenterPaSvarFraNAV = response.getFacetQuery().get(venterPaSvarFraNAV);
            long antallVenterPaSvarFraBruker = response.getFacetQuery().get(venterPaSvarFraBruker);
            statusTall
                    .setTotalt(antallTotalt)
                    .setInaktiveBrukere(antallInaktiveBrukere)
                    .setNyeBrukere(antallNyeBrukere)
                    .setVenterPaSvarFraNAV(antallVenterPaSvarFraNAV)
                    .setVenterPaSvarFraBruker(antallVenterPaSvarFraBruker);
        } catch (SolrServerException | IOException e) {
            logger.error("Henting av statustall for portefølje feilet ", e.getMessage(), e);
        }

        return statusTall;
    }

    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        String inaktiveBrukere = "formidlingsgruppekode:ISERV";
        String venterPaSvarFraNAV = "venterpasvarfranav:*";
        String venterPaSvarFraBruker = "venterpasvarfrabruker:*";

        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFilterQuery("veileder_id:" + veilederIdent);
        solrQuery.addFacetQuery(inaktiveBrukere);
        solrQuery.addFacetQuery(venterPaSvarFraNAV);
        solrQuery.addFacetQuery(venterPaSvarFraBruker);
        solrQuery.setRows(0);

        StatusTall statusTall = new StatusTall();
        QueryResponse response;
        try {
            response = solrClientSlave.query(solrQuery);
            long antallTotalt = response.getResults().getNumFound();
            long antallInaktiveBrukere = response.getFacetQuery().get(inaktiveBrukere);
            long antallVenterPaSvarFraNAV = response.getFacetQuery().get(venterPaSvarFraNAV);
            long antallVenterPaSvarFraBruker = response.getFacetQuery().get(venterPaSvarFraBruker);
            statusTall
                    .setTotalt(antallTotalt)
                    .setInaktiveBrukere(antallInaktiveBrukere)
                    .setVenterPaSvarFraNAV(antallVenterPaSvarFraNAV)
                    .setVenterPaSvarFraBruker(antallVenterPaSvarFraBruker);
        } catch (SolrServerException | IOException e) {
            logger.error("Henting av statustall for veileder feilet ", e.getMessage(), e);
        }

        return statusTall;
    }
}
