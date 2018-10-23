package no.nav.fo.service;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import no.nav.fo.util.BatchConsumer;
import no.nav.fo.util.SolrUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.AktivitetUtils.applyAktivitetStatuser;
import static no.nav.fo.util.AktivitetUtils.applyTiltak;
import static no.nav.fo.util.BatchConsumer.batchConsumer;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.SolrSortUtils.addPaging;
import static no.nav.fo.util.SolrUtils.harIkkeVeilederFilter;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class SolrServiceImpl implements SolrService {

    private static final String HOVEDINDEKSERING = "Hovedindeksering";
    private static final String DELTAINDEKSERING = "Deltaindeksering";

    public static final String DATOFILTER_PROPERTY = "arena.aktivitet.datofilter";

    private SolrClient solrClientSlave;
    private SolrClient solrClientMaster;
    private BrukerRepository brukerRepository;
    private AktivitetDAO aktivitetDAO;
    private AktoerService aktoerService;
    private VeilederService veilederService;
    private Executor executor;
    private LockService lockService;

    @Inject
    public SolrServiceImpl(
            @Named("solrClientMaster") SolrClient solrClientMaster,
            @Named("solrClientSlave") SolrClient solrClientSlave,
            BrukerRepository brukerRepository,
            AktoerService aktoerService,
            VeilederService veilederService,
            AktivitetDAO aktivitetDAO,
            LockService lockService) {

        this.solrClientMaster = solrClientMaster;
        this.solrClientSlave = solrClientSlave;
        this.brukerRepository = brukerRepository;
        this.aktivitetDAO = aktivitetDAO;
        this.aktoerService = aktoerService;
        this.veilederService = veilederService;
        this.executor = Executors.newFixedThreadPool(5);
        this.lockService = lockService;
    }

    @Transactional
    @Override
    public void hovedindeksering() {
        lockService.runWithLock(this::hovedindekseringWithLock);
    }

    private void hovedindekseringWithLock() {
        log.info("Indeksering: Starter hovedindeksering...");
        LocalDateTime t0 = LocalDateTime.now();

        final int[] antallBrukere = {0};
        deleteAllDocuments();
        commit();

        BatchConsumer<SolrInputDocument> consumer = batchConsumer(10000, (dokumenter) -> {
            antallBrukere[0] += dokumenter.size();
            indekserDokumenter(dokumenter);
        });
        brukerRepository.prosesserBrukere(BrukerRepository::erOppfolgingsBruker, consumer);
        consumer.flush(); // Må kalles slik at batcher mindre enn `size` også blir prosessert.

        commit();
        brukerRepository.updateIndeksertTidsstempel(Timestamp.valueOf(t0));

        logFerdig(t0, antallBrukere[0], HOVEDINDEKSERING);
    }

    @Transactional
    @Override
    public void deltaindeksering() {
        lockService.runWithLock(this::deltaindekseringWithLock);
    }

    private void deltaindekseringWithLock() {
        log.info("Indeksering: Starter deltaindeksering");
        LocalDateTime t0 = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(t0);

        List<SolrInputDocument> dokumenter = brukerRepository.retrieveOppdaterteBrukere();
        if (dokumenter.isEmpty()) {
            log.info("Ingen nye dokumenter i databasen");
            return;
        }

        List<SolrInputDocument> oppfolgingsbrukere = dokumenter.stream()
                .filter(BrukerRepository::erOppfolgingsBruker)
                .collect(toList());

        indekserDokumenter(oppfolgingsbrukere);

        dokumenter.stream()
                .filter((bruker) -> !BrukerRepository.erOppfolgingsBruker(bruker))
                .forEach((bruker) -> slettBruker((String) bruker.get("fnr").getValue()));

        commit();
        brukerRepository.updateIndeksertTidsstempel(timestamp);

        int antall = dokumenter.size();
        Event event = MetricsFactory.createEvent("deltaindeksering.fullfort");
        event.addFieldToReport("antall.oppdateringer", antall);
        event.report();
        logFerdig(t0, antall, DELTAINDEKSERING);
    }

    private void indekserDokumenter(List<SolrInputDocument> dokumenter) {
        leggDataTilSolrDocument(dokumenter);
        addDocumentsToIndex(dokumenter);
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        String queryString = byggQueryString(enhetId, veilederIdent);
        boolean sorterNyeForVeileder = veilederIdent.map(StringUtils::isNotBlank).orElse(false);

        List<VeilederId> veiledere = veilederService.getIdenter(enhetId);

        SolrQuery solrQuery = SolrUtils.buildSolrQuery(queryString, sorterNyeForVeileder, veiledere, sortOrder, sortField, filtervalg);
        addPaging(solrQuery, fra, antall);
        QueryResponse response = timed("solr.hentbrukere", () -> Try.of(() -> solrClientSlave.query(solrQuery)).get());
        SolrUtils.checkSolrResponseCode(response.getStatus());
        SolrDocumentList results = response.getResults();
        List<Bruker> brukere = results.stream().map(Bruker::of).collect(toList());
        int antallBrukere = Long.valueOf(response.getResults().getNumFound()).intValue();
        return new BrukereMedAntall(antallBrukere, brukere);
    }

    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        return hentBrukere(enhetId, veilederIdent, sortOrder, sortField, filtervalg, null, null);
    }

    String byggQueryString(String enhetId, Optional<String> veilederIdent) {
        return veilederIdent
                .map((ident) -> isBlank(ident) ? null : ident)
                .map((ident) -> "veileder_id: " + ident + " AND enhet_id: " + enhetId)
                .orElse("enhet_id: " + enhetId);
    }

    private void leggDataTilSolrDocument(List<SolrInputDocument> dokumenter) {
        Boolean batch = dokumenter.size() > 1;
        BiConsumer<Timer, Boolean> tagsAppeder = (timer, success) -> timer.addTagToReport("batch", batch.toString());
        timed("indeksering.applyaktiviteter", () -> applyAktivitetStatuser(dokumenter, aktivitetDAO), tagsAppeder);
        timed("indeksering.applytiltak", () -> applyTiltak(dokumenter, aktivitetDAO), tagsAppeder);
    }

    @Override
    public void slettBruker(String fnr) {
        deleteDocuments("fnr:" + fnr);
    }

    @Override
    public void slettBruker(PersonId personid) {
        deleteDocuments("person_id:" + personid.toString());
    }

    @Override
    public FacetResults hentPortefoljestorrelser(String enhetId) {

        String facetFieldString = "veileder_id";

        SolrQuery solrQuery = SolrUtils.buildSolrFacetQuery("enhet_id: " + enhetId, facetFieldString);
        solrQuery.setRows(0);
        // ikke interessert i veiledere som ikke har tilordnede brukere
        solrQuery.setFacetMinCount(1);

        QueryResponse response = new QueryResponse();
        try {
            response = solrClientSlave.query(solrQuery);
        } catch (SolrServerException | IOException e) {
            log.error("Spørring mot solrindeks feilet: " + solrQuery, e);
        }

        FacetField facetField = response.getFacetField(facetFieldString);

        return SolrUtils.mapFacetResults(facetField);
    }

    @Override
    public void indekserBrukerdata(PersonId personId) {
        SolrInputDocument brukerDokument = brukerRepository.retrieveBrukermedBrukerdata(personId.toString());
        if (!BrukerRepository.erOppfolgingsBruker(brukerDokument)) {
            log.info("Sletter bruker med personId {} fra indeksen ", personId);
            slettBruker(personId);
        } else {
            log.info("Legger bruker med personId {} til i indeksen ", personId);
            leggDataTilSolrDocument(singletonList(brukerDokument));
            addDocumentsToIndex(singletonList(brukerDokument));
        }
        commit();
        log.info("Indeks oppdatert for person med personId {}", personId);
    }

    @Override
    public void indekserBrukere(List<PersonId> personIds) {
        List<SolrInputDocument> dokumenter = brukerRepository.retrieveBrukeremedBrukerdata(personIds).stream()
                .filter(BrukerRepository::erOppfolgingsBruker)
                .collect(toList());
        indekserDokumenter(dokumenter);
        commit();
    }

    private void indekserBrukerdata(AktoerId aktoerId) {
        aktoerService
                .hentPersonidFraAktoerid(aktoerId)
                .onSuccess(this::indekserBrukerdata);
    }

    @Override
    public void indekserAsynkront(AktoerId aktoerId) {
        CompletableFuture.runAsync(() -> indekserBrukerdata(aktoerId), executor);
    }

    @Override
    public void commit() {
        String feilmeldingsTekst = "Kunne ikke gjennomføre commit til solrindeksen.";
        Try.of(() -> solrClientMaster.commit())
                .onFailure(e -> {
                    if (e instanceof HttpSolrClient.RemoteSolrException) {
                        log.warn(feilmeldingsTekst, e.getMessage());
                    } else {
                        log.error(feilmeldingsTekst, e);
                    }
                });
    }

    private List<SolrInputDocument> addDocumentsToIndex(List<SolrInputDocument> dokumenter) {
        // javaslang.collection-API brukes her pga sliding-metoden
        return timed("indeksering.adddocumentstoindex", () -> {
            io.vavr.collection.List.ofAll(dokumenter)
                    .sliding(10000, 10000)
                    .forEach(docs -> {
                        try {
                            solrClientMaster.add(docs.toJavaList());
                            log.info(format("Legger til %d dokumenter i indeksen", docs.length()));
                        } catch (SolrServerException | IOException e) {
                            log.error("Legge til solrdokumenter før commit feilet.", e);
                        }
                    });
            return dokumenter;
        });
    }

    private void deleteAllDocuments() {
        deleteDocuments("*:*");
    }

    private void deleteDocuments(String query) {
        try {
            UpdateResponse response = solrClientMaster.deleteByQuery(query);
            SolrUtils.checkSolrResponseCode(response.getStatus());
        } catch (SolrServerException | IOException | SolrUpdateResponseCodeException e) {
            log.error("Kunne ikke slette dokumenter fra solrindeks: " + query, e);
        }
    }

    private void logFerdig(LocalDateTime t0, int antall, String indekseringstype) {
        Duration duration = Duration.between(t0, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds();
        String logString = format("Indeksering: %s fullført! | Tid brukt(hh:mm:ss): %02d:%02d:%02d | Dokumenter oppdatert: %d", indekseringstype, hours, minutes, seconds, antall);
        log.info(logString);
    }

    @Override
    public StatusTall hentStatusTallForPortefolje(String enhet) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        Optional<String> maybeUfordelteBrukere = harIkkeVeilederFilter(veilederService.getIdenter(enhet));

        String inaktiveBrukere = "formidlingsgruppekode:ISERV";
        String venterPaSvarFraNAV = "venterpasvarfranav:*";
        String venterPaSvarFraBruker = "venterpasvarfrabruker:*";
        String iavtaltAktivitet = "aktiviteter:*";
        String ikkeIAvtaltAktivitet = "-aktiviteter:*";
        String utlopteAktiviteter = "nyesteutlopteaktivitet:*";
        String trengerVurdering = "trenger_vurdering:true";


        solrQuery.addFilterQuery("enhet_id:" + enhet);

        maybeUfordelteBrukere.ifPresent(solrQuery::addFacetQuery);

        solrQuery.addFacetQuery(inaktiveBrukere);
        solrQuery.addFacetQuery(venterPaSvarFraNAV);
        solrQuery.addFacetQuery(venterPaSvarFraBruker);
        solrQuery.addFacetQuery(iavtaltAktivitet);
        solrQuery.addFacetQuery(ikkeIAvtaltAktivitet);
        solrQuery.addFacetQuery(utlopteAktiviteter);
        solrQuery.addFacetQuery(trengerVurdering);
        solrQuery.setRows(0);

        StatusTall statusTall = new StatusTall();
        QueryResponse response;

        response = timed("solr.statustall.enhet", () -> getResponse(solrQuery));

        long antallTotalt = response.getResults().getNumFound();
        long antallUfordelteBrukere = maybeUfordelteBrukere.map(ufordelteBrukere ->
                response.getFacetQuery().get(ufordelteBrukere)).orElse(0);
        long antallInaktiveBrukere = response.getFacetQuery().get(inaktiveBrukere);
        long antallVenterPaSvarFraNAV = response.getFacetQuery().get(venterPaSvarFraNAV);
        long antallVenterPaSvarFraBruker = response.getFacetQuery().get(venterPaSvarFraBruker);
        long antalliavtaltAktivitet = response.getFacetQuery().get(iavtaltAktivitet);
        long antallIkkeIAvtaltAktivitet = response.getFacetQuery().get(ikkeIAvtaltAktivitet);
        long antallUtlopteAktiviteter = response.getFacetQuery().get(utlopteAktiviteter);
        long antallTrengerVurdering = response.getFacetQuery().get(trengerVurdering);


        statusTall
                .setTotalt(antallTotalt)
                .setInaktiveBrukere(antallInaktiveBrukere)
                .setNyeBrukere(antallUfordelteBrukere)
                .setUfordelteBrukere(antallUfordelteBrukere)
                .setVenterPaSvarFraNAV(antallVenterPaSvarFraNAV)
                .setVenterPaSvarFraBruker(antallVenterPaSvarFraBruker)
                .setIavtaltAktivitet(antalliavtaltAktivitet)
                .setIkkeIavtaltAktivitet(antallIkkeIAvtaltAktivitet)
                .setUtlopteAktiviteter(antallUtlopteAktiviteter)
                .setTrengerVurdering(antallTrengerVurdering);

        return statusTall;
    }

    @SneakyThrows
    private QueryResponse getResponse(SolrQuery solrQuery) {
        return solrClientSlave.query(solrQuery);
    }

    @Override
    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        String nyForVeileder = "ny_for_veileder:true";
        String trengerVurdering = "trenger_vurdering:true";
        String inaktiveBrukere = "formidlingsgruppekode:ISERV";
        String venterPaSvarFraNAV = "venterpasvarfranav:*";
        String venterPaSvarFraBruker = "venterpasvarfrabruker:*";
        String iavtaltAktivitet = "aktiviteter:*";
        String ikkeIAvtaltAktivitet = "-aktiviteter:*";
        String utlopteAktiviteter = "nyesteutlopteaktivitet:*";
        String minArbeidsliste = "arbeidsliste_aktiv:*";

        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFilterQuery("veileder_id:" + veilederIdent);
        solrQuery.addFacetQuery(nyForVeileder);
        solrQuery.addFacetQuery(trengerVurdering);
        solrQuery.addFacetQuery(inaktiveBrukere);
        solrQuery.addFacetQuery(venterPaSvarFraNAV);
        solrQuery.addFacetQuery(venterPaSvarFraBruker);
        solrQuery.addFacetQuery(iavtaltAktivitet);
        solrQuery.addFacetQuery(ikkeIAvtaltAktivitet);
        solrQuery.addFacetQuery(utlopteAktiviteter);
        solrQuery.addFacetQuery(minArbeidsliste);

        solrQuery.setRows(0);

        StatusTall statusTall = new StatusTall();
        QueryResponse response;
        try {
            response = solrClientSlave.query(solrQuery);
            long antallTotalt = response.getResults().getNumFound();
            long antallInaktiveBrukere = response.getFacetQuery().get(inaktiveBrukere);
            long antallVenterPaSvarFraNAV = response.getFacetQuery().get(venterPaSvarFraNAV);
            long antallVenterPaSvarFraBruker = response.getFacetQuery().get(venterPaSvarFraBruker);
            long antalliavtaltAktivitet = response.getFacetQuery().get(iavtaltAktivitet);
            long antallIkkeIAvtaltAktivitet = response.getFacetQuery().get(ikkeIAvtaltAktivitet);
            long antallUtlopteAktiviteter = response.getFacetQuery().get(utlopteAktiviteter);
            long antallIarbeidsliste = response.getFacetQuery().get(minArbeidsliste);
            long antallNyeBrukerForVeileder = response.getFacetQuery().get(nyForVeileder);
            long antallTrengerVurdering = response.getFacetQuery().get(trengerVurdering);
            statusTall
                    .setTotalt(antallTotalt)
                    .setInaktiveBrukere(antallInaktiveBrukere)
                    .setVenterPaSvarFraNAV(antallVenterPaSvarFraNAV)
                    .setVenterPaSvarFraBruker(antallVenterPaSvarFraBruker)
                    .setIavtaltAktivitet(antalliavtaltAktivitet)
                    .setIkkeIavtaltAktivitet(antallIkkeIAvtaltAktivitet)
                    .setUtlopteAktiviteter(antallUtlopteAktiviteter)
                    .setMinArbeidsliste(antallIarbeidsliste)
                    .setNyeBrukereForVeileder(antallNyeBrukerForVeileder)
                    .setTrengerVurdering(antallTrengerVurdering);
        } catch (SolrServerException | IOException e) {
            log.error("Henting av statustall for veilederportefølje feilet: " + solrQuery, e);
        }

        return statusTall;
    }

    @Override
    public Try<List<Bruker>> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery("veileder_id:" + veilederId.toString());
        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFilterQuery("arbeidsliste_aktiv:true");

        return Try.of(() -> solrClientSlave.query(solrQuery))
                .map(res -> res.getResults().stream().map(Bruker::of).collect(toList()))
                .onFailure(e -> log.warn("Henting av brukere med arbeidsliste feilet: {}", e.getMessage()));
    }

}
