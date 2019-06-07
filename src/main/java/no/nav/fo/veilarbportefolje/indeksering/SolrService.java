package no.nav.fo.veilarbportefolje.indeksering;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.exception.SolrUpdateResponseCodeException;
import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.fo.veilarbportefolje.util.BatchConsumer;
import no.nav.fo.veilarbportefolje.util.MetricsUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
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
import static no.nav.fo.veilarbportefolje.indeksering.SolrSortUtils.addPaging;
import static no.nav.fo.veilarbportefolje.indeksering.SolrUtils.harIkkeVeilederFilter;
import static no.nav.fo.veilarbportefolje.util.AktivitetUtils.applyAktivitetStatuser;
import static no.nav.fo.veilarbportefolje.util.AktivitetUtils.applyTiltak;
import static no.nav.fo.veilarbportefolje.util.BatchConsumer.batchConsumer;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;

@Slf4j
public class SolrService implements IndekseringService {

    private static final String HOVEDINDEKSERING = "Hovedindeksering";
    private static final String DELTAINDEKSERING = "Deltaindeksering";

    private SolrClient solrClientSlave;
    private SolrClient solrClientMaster;
    private BrukerRepository brukerRepository;
    private AktivitetDAO aktivitetDAO;
    private AktoerService aktoerService;
    private VeilederService veilederService;
    private Executor executor;
    private PepClient pepClient;

    @Inject
    public SolrService(
            @Named("solrClientMaster") SolrClient solrClientMaster,
            @Named("solrClientSlave") SolrClient solrClientSlave,
            BrukerRepository brukerRepository,
            AktoerService aktoerService,
            VeilederService veilederService,
            AktivitetDAO aktivitetDAO,
            PepClient pepClient) {

        this.solrClientMaster = solrClientMaster;
        this.solrClientSlave = solrClientSlave;
        this.brukerRepository = brukerRepository;
        this.aktivitetDAO = aktivitetDAO;
        this.aktoerService = aktoerService;
        this.veilederService = veilederService;
        this.executor = Executors.newFixedThreadPool(5);
        this.pepClient = pepClient;
    }

    @Transactional
    @Override
    public void hovedindeksering() {
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

    public void indekserDokumenter(List<SolrInputDocument> dokumenter) {
        leggDataTilSolrDocument(dokumenter);
        addDocumentsToIndex(dokumenter);
    }

    @Override
    public BrukereMedAntall hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg, Integer fra, Integer antall) {
        List<VeilederId> veiledere = veilederService.getIdenter(enhetId);
        SolrQuery solrQuery = SolrUtils.buildSolrQuery(enhetId, veilederIdent, veiledere, sortOrder, sortField, filtervalg, pepClient);
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

    private void leggDataTilSolrDocument(List<SolrInputDocument> dokumenter) {
        Boolean batch = dokumenter.size() > 1;
        BiConsumer<Timer, Boolean> tagsAppeder = (timer, success) -> timer.addTagToReport("batch", batch.toString());
        timed("indeksering.applyaktiviteter", () -> applyAktivitetStatuser(dokumenter, aktivitetDAO), tagsAppeder);
        timed("indeksering.applytiltak", () -> applyTiltak(dokumenter, aktivitetDAO), tagsAppeder);
    }

    public void slettBruker(String fnr) {
        deleteDocuments("fnr:" + fnr);
    }


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

        QueryResponse response = MetricsUtils.timed("solr.hentportefoljestorrelser", () -> getResponse(solrQuery));
        FacetField facetField = response.getFacetField(facetFieldString);

        return SolrUtils.mapFacetResults(facetField);
    }

    void indekserBrukerdata(PersonId personId) {
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
        String erSykmeldtMedArbeidsgiver  = SolrUtils.formidlingsgruppekodeOgKvalifiseringsgruppeKoderErSykmeldtMedArbeidsgiver();


        solrQuery.addFilterQuery("enhet_id:" + enhet);

        maybeUfordelteBrukere.ifPresent(solrQuery::addFacetQuery);

        solrQuery.addFacetQuery(inaktiveBrukere);
        solrQuery.addFacetQuery(venterPaSvarFraNAV);
        solrQuery.addFacetQuery(venterPaSvarFraBruker);
        solrQuery.addFacetQuery(iavtaltAktivitet);
        solrQuery.addFacetQuery(ikkeIAvtaltAktivitet);
        solrQuery.addFacetQuery(utlopteAktiviteter);
        solrQuery.addFacetQuery(trengerVurdering);
        solrQuery.addFacetQuery(erSykmeldtMedArbeidsgiver );
        solrQuery.setRows(0);

        StatusTall statusTall = new StatusTall();
        QueryResponse response = timed("solr.statustall.enhet", () -> getResponse(solrQuery));

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
        long antallErSykmeldtMedArbeidsgiver = response.getFacetQuery().get(erSykmeldtMedArbeidsgiver);


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
                .setTrengerVurdering(antallTrengerVurdering)
                .setErSykmeldtMedArbeidsgiver(antallErSykmeldtMedArbeidsgiver);

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
        String erSykmeldtMedArbeidsgiver  = SolrUtils.formidlingsgruppekodeOgKvalifiseringsgruppeKoderErSykmeldtMedArbeidsgiver() ;

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
        solrQuery.addFacetQuery(erSykmeldtMedArbeidsgiver );

        solrQuery.setRows(0);

        StatusTall statusTall = new StatusTall();
        QueryResponse response = timed("solr.statustall.veileder", () -> getResponse(solrQuery));
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
        long antallErSykmeldtMedArbeidsgiver = response.getFacetQuery().get(erSykmeldtMedArbeidsgiver);
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
                .setTrengerVurdering(antallTrengerVurdering)
                .setErSykmeldtMedArbeidsgiver(antallErSykmeldtMedArbeidsgiver);

        return statusTall;
    }

    @Override
    public List<Bruker> hentBrukereMedArbeidsliste(VeilederId veilederId, String enhet) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addFilterQuery("veileder_id:" + veilederId.toString());
        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFilterQuery("arbeidsliste_aktiv:true");

        QueryResponse response = timed("solr.hentarbeidsliste", () -> getResponse(solrQuery));
        return response.getResults().stream()
                .map(Bruker::of)
                .collect(toList());
    }

}
