package no.nav.fo.service;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import no.nav.fo.util.AktivitetUtils;
import no.nav.fo.util.BatchConsumer;
import no.nav.fo.util.SolrUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.AktivitetUtils.applyAktivitetStatuser;
import static no.nav.fo.util.AktivitetUtils.applyTiltak;
import static no.nav.fo.util.BatchConsumer.batchConsumer;
import static no.nav.fo.util.DateUtils.toUtcString;
import static no.nav.fo.util.MetricsUtils.timed;
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
    private ArbeidslisteRepository arbeidslisteRepository;
    private AktoerService aktoerService;
    private Timestamp datofilterTiltak;

    @Inject
    public SolrServiceImpl(
            @Named("solrClientMaster") SolrClient solrClientMaster,
            @Named("solrClientSlave") SolrClient solrClientSlave,
            BrukerRepository brukerRepository,
            ArbeidslisteRepository arbeidslisteRepository,
            AktoerService aktoerService,
            AktivitetDAO aktivitetDAO
    ) {

        this.solrClientMaster = solrClientMaster;
        this.solrClientSlave = solrClientSlave;
        this.brukerRepository = brukerRepository;
        this.aktivitetDAO = aktivitetDAO;
        this.arbeidslisteRepository = arbeidslisteRepository;
        this.aktoerService = aktoerService;
        this.datofilterTiltak = AktivitetUtils.parseDato(System.getProperty(DATOFILTER_PROPERTY));
    }

    @Transactional
    @Override
    public void hovedindeksering() {


        if (SolrUtils.isSlaveNode()) {
            log.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        log.info("Starter hovedindeksering");
        LocalDateTime t0 = LocalDateTime.now();

        final int[] antallBrukere = {0};
        deleteAllDocuments();
        commit();

        BatchConsumer<SolrInputDocument> consumer = batchConsumer(10000, (dokumenter) -> {
            antallBrukere[0] += dokumenter.size();
            leggDataTilSolrDocument(dokumenter);
            addDocumentsToIndex(dokumenter);
        });
        brukerRepository.prosesserBrukere(BrukerRepository::erOppfolgingsBruker, consumer);
        consumer.flush(); // Må kalles slik at batcher mindre enn `size` også blir prosessert.

        commit();
        brukerRepository.updateTidsstempel(Timestamp.valueOf(t0));

        logFerdig(t0, antallBrukere[0], HOVEDINDEKSERING);
    }

    @Scheduled(cron = "${veilarbportefolje.cron.deltaindeksering}")
    @Transactional
    @Override
    public void deltaindeksering() {
        if (SolrUtils.isSlaveNode()) {
            log.info("Noden er en slave. Kun masternoden kan iverksett indeksering. Avbryter.");
            return;
        }

        log.info("Starter deltaindeksering");
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

        leggDataTilSolrDocument(oppfolgingsbrukere);
        addDocumentsToIndex(oppfolgingsbrukere);

        dokumenter.stream()
                .filter((bruker) -> !BrukerRepository.erOppfolgingsBruker(bruker))
                .forEach((bruker) -> slettBruker((String) bruker.get("fnr").getValue()));

        commit();
        brukerRepository.updateTidsstempel(timestamp);

        int antall = dokumenter.size();
        Event event = MetricsFactory.createEvent("deltaindeksering.fullfort");
        event.addFieldToReport("antall.oppdateringer", antall);
        event.report();
        logFerdig(t0, antall, DELTAINDEKSERING);
    }

    public void populerIndeksForPersonids(List<PersonId> personIds) {
        List<SolrInputDocument> dokumenter = brukerRepository.retrieveBrukeremedBrukerdata(personIds);
        indekserDokumenter(dokumenter);
    }

    public void indekserDokumenter(List<SolrInputDocument> dokumenter) {
        leggDataTilSolrDocument(dokumenter);
        addDocumentsToIndex(dokumenter);
    }

    @Override
    public List<Bruker> hentBrukere(String enhetId, Optional<String> veilederIdent, String sortOrder, String sortField, Filtervalg filtervalg) {
        String queryString = byggQueryString(enhetId, veilederIdent);
        return hentBrukere(queryString, sortOrder, sortField, filtervalg);
    }

    @Override
    public Either<Throwable, List<Bruker>> query(String query) {
        return Try.of(() -> {
            SolrQuery solrQuery = new SolrQuery("*:*");
            solrQuery.addFilterQuery(query);
            QueryResponse response = solrClientSlave.query(solrQuery);
            SolrUtils.checkSolrResponseCode(response.getStatus());
            SolrDocumentList results = response.getResults();
            return results.stream().map(Bruker::of).collect(toList());
        }).toEither();
    }

    public String byggQueryString(String enhetId, Optional<String> veilederIdent) {
        return veilederIdent
                .map((ident) -> isBlank(ident) ? null : ident)
                .map((ident) -> "veileder_id: " + ident + " AND enhet_id: " + enhetId)
                .orElse("enhet_id: " + enhetId);
    }

    private void leggDataTilSolrDocument(List<SolrInputDocument> dokumenter) {
        Boolean batch = dokumenter.size() > 1;
        BiConsumer<Timer, Boolean> tagsAppeder = (timer, success) -> timer.addTagToReport("batch", batch.toString());
        timed("indeksering.applyaktiviteter", () -> applyAktivitetStatuser(dokumenter, aktivitetDAO), tagsAppeder);
        timed("indeksering.applyarbeidslistedata", () -> applyArbeidslisteData(dokumenter, arbeidslisteRepository, aktoerService), tagsAppeder);
        timed("indeksering.applytiltak", () -> applyTiltak(dokumenter, aktivitetDAO), tagsAppeder);
    }

    static void applyArbeidslisteData(List<SolrInputDocument> brukere, ArbeidslisteRepository arbeidslisteRepository, AktoerService aktoerService) {
        List<PersonId> personIds = brukere.stream().map((dokument) -> PersonId.of((String) dokument.get("person_id").getValue())).collect(toList());
        Map<PersonId, Optional<AktoerId>> personIdToAktoerId = aktoerService.hentAktoeridsForPersonids(personIds);
        List<AktoerId> aktoerids = personIdToAktoerId.values().stream().filter(Optional::isPresent).map(Optional::get).collect(toList());

        Map<AktoerId, Optional<Arbeidsliste>> arbeidslisteMap = arbeidslisteRepository.retrieveArbeidsliste(aktoerids);

        brukere.forEach((dokument) -> {
            PersonId personId = PersonId.of((String) dokument.get("person_id").getValue());
            Optional<Arbeidsliste> arbeidsliste = Optional.of(personId).flatMap(personIdToAktoerId::get).flatMap(arbeidslisteMap::get);
            if (arbeidsliste.isPresent()) {
                dokument.setField("arbeidsliste_aktiv", true);
                dokument.setField("arbeidsliste_sist_endret_av_veilederid", arbeidsliste.get().getSistEndretAv().toString());
                dokument.setField("arbeidsliste_endringstidspunkt", toUtcString(arbeidsliste.get().getEndringstidspunkt()));
                dokument.setField("arbeidsliste_kommentar", arbeidsliste.get().getKommentar());
                dokument.setField("arbeidsliste_frist", toUtcString(arbeidsliste.get().getFrist()));
                dokument.setField("arbeidsliste_er_oppfolgende_veileder", arbeidsliste.get().getIsOppfolgendeVeileder());
            }
        });
    }

    private List<Bruker> hentBrukere(String queryString, String sortOrder, String sortField, Filtervalg filtervalg) {
        List<Bruker> brukere = new ArrayList<>();
        SolrQuery solrQuery = SolrUtils.buildSolrQuery(queryString, filtervalg);
        try {
            QueryResponse response = solrClientSlave.query(solrQuery);
            SolrUtils.checkSolrResponseCode(response.getStatus());
            SolrDocumentList results = response.getResults();
            brukere = results.stream().map(Bruker::of).collect(toList());
        } catch (SolrServerException | IOException e) {
            log.error("Spørring mot solrindeks feilet: {}", solrQuery.toString(), e);
        }
        return SolrUtils.sortBrukere(brukere, sortOrder, sortField, filtervalg);
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
    public void slettBrukere(List<PersonId> personIds) {
        personIds.forEach(this::slettBruker);
    }

    @Override
    public FacetResults hentPortefoljestorrelser(String enhetId) {

        String facetFieldString = "veileder_id";

        SolrQuery solrQuery = SolrUtils.buildSolrFacetQuery("enhet_id: " + enhetId, facetFieldString);

        QueryResponse response = new QueryResponse();
        try {
            response = solrClientSlave.query(solrQuery);
            log.debug(response.toString());
        } catch (SolrServerException | IOException e) {
            log.error("Spørring mot solrindeks feilet: {}", solrQuery.toString(), e);
        }

        FacetField facetField = response.getFacetField(facetFieldString);

        return SolrUtils.mapFacetResults(facetField);
    }

    @Override
    public void indekserBrukerdata(PersonId personId) {
        SolrInputDocument brukerDokument = brukerRepository.retrieveBrukermedBrukerdata(personId.toString());
        if (!BrukerRepository.erOppfolgingsBruker(brukerDokument)) {
            return;
        }
        log.info("Legger bruker med personId {} til i indeksen ", personId);

        leggDataTilSolrDocument(singletonList(brukerDokument));
        addDocumentsToIndex(singletonList(brukerDokument));
        commit();
        log.info("Bruker med personId {} lagt til i indeksen", personId);
    }

    @Override
    public void indekserBrukerdata(AktoerId aktoerId) {
        aktoerService
                .hentPersonidFraAktoerid(aktoerId)
                .onSuccess(this::indekserBrukerdata);
    }

    @Override
    public Try<UpdateResponse> commit() {
        return Try.of(() -> solrClientMaster.commit())
                .onFailure(e -> log.error("Kunne ikke gjennomføre commit til solrindeksen.", e));
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
            log.error("Kunne ikke slette dokumenter fra solrindeks: {}", query, e);
        }
    }

    private void logFerdig(LocalDateTime t0, int antall, String indekseringstype) {
        Duration duration = Duration.between(t0, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds();
        String logString = format("%s fullført! | Tid brukt(hh:mm:ss): %02d:%02d:%02d | Dokumenter oppdatert: %d", indekseringstype, hours, minutes, seconds, antall);
        log.info(logString);
    }

    @Override
    public StatusTall hentStatusTallForPortefolje(String enhet) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        String nyeBrukere = "-veileder_id:*";
        String inaktiveBrukere = "formidlingsgruppekode:ISERV";
        String venterPaSvarFraNAV = "venterpasvarfranav:*";
        String venterPaSvarFraBruker = "venterpasvarfrabruker:*";
        String iavtaltAktivitet = "aktiviteter:*";
        String ikkeIAvtaltAktivitet = "-aktiviteter:*";
        String utlopteAktiviteter = "nyesteutlopteaktivitet:*";

        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFacetQuery(nyeBrukere);
        solrQuery.addFacetQuery(inaktiveBrukere);
        solrQuery.addFacetQuery(venterPaSvarFraNAV);
        solrQuery.addFacetQuery(venterPaSvarFraBruker);
        solrQuery.addFacetQuery(iavtaltAktivitet);
        solrQuery.addFacetQuery(ikkeIAvtaltAktivitet);
        solrQuery.addFacetQuery(utlopteAktiviteter);
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
            long antalliavtaltAktivitet = response.getFacetQuery().get(iavtaltAktivitet);
            long antallIkkeIAvtaltAktivitet = response.getFacetQuery().get(ikkeIAvtaltAktivitet);
            long antallUtlopteAktiviteter = response.getFacetQuery().get(utlopteAktiviteter);
            statusTall
                    .setTotalt(antallTotalt)
                    .setInaktiveBrukere(antallInaktiveBrukere)
                    .setNyeBrukere(antallNyeBrukere)
                    .setVenterPaSvarFraNAV(antallVenterPaSvarFraNAV)
                    .setVenterPaSvarFraBruker(antallVenterPaSvarFraBruker)
                    .setIavtaltAktivitet(antalliavtaltAktivitet)
                    .setIkkeIavtaltAktivitet(antallIkkeIAvtaltAktivitet)
                    .setUtlopteAktiviteter(antallUtlopteAktiviteter);
        } catch (SolrServerException | IOException e) {
            log.error("Henting av statustall for enhetsportefølje feilet: {}", solrQuery, e);
        }

        return statusTall;
    }

    @Override
    public StatusTall hentStatusTallForVeileder(String enhet, String veilederIdent) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        String inaktiveBrukere = "formidlingsgruppekode:ISERV";
        String venterPaSvarFraNAV = "venterpasvarfranav:*";
        String venterPaSvarFraBruker = "venterpasvarfrabruker:*";
        String iavtaltAktivitet = "aktiviteter:*";
        String ikkeIAvtaltAktivitet = "-aktiviteter:*";
        String utlopteAktiviteter = "nyesteutlopteaktivitet:*";
        String minArbeidsliste = "arbeidsliste_aktiv:*";

        solrQuery.addFilterQuery("enhet_id:" + enhet);
        solrQuery.addFilterQuery("veileder_id:" + veilederIdent);
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
            statusTall
                    .setTotalt(antallTotalt)
                    .setInaktiveBrukere(antallInaktiveBrukere)
                    .setVenterPaSvarFraNAV(antallVenterPaSvarFraNAV)
                    .setVenterPaSvarFraBruker(antallVenterPaSvarFraBruker)
                    .setIavtaltAktivitet(antalliavtaltAktivitet)
                    .setIkkeIavtaltAktivitet(antallIkkeIAvtaltAktivitet)
                    .setUtlopteAktiviteter(antallUtlopteAktiviteter)
                    .setMinArbeidsliste(antallIarbeidsliste);
        } catch (SolrServerException | IOException e) {
            log.error("Henting av statustall for veilederportefølje feilet: {}", solrQuery, e);
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
