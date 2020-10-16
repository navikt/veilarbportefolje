package no.nav.pto.veilarbportefolje.admin;

import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.util.RunningJob;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static no.nav.pto.veilarbportefolje.util.JobUtils.runAsyncJob;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final List<String> admins;
    private final ElasticIndexer elasticIndexer;
    private final AktivitetService aktivitetService;
    private final AktorregisterClient aktorregisterClient;
    private final TiltakHandler tiltakHandler;
    private final KopierGR199FraArena kopierGR199FraArena;

    public AdminController(EnvironmentProperties environmentProperties,
                           ElasticIndexer elasticIndexer,
                           AktivitetService aktivitetService,
                           AktorregisterClient aktorregisterClient,
                           TiltakHandler tiltakHandler,
                           KopierGR199FraArena kopierGR199FraArena) {
        this.admins = environmentProperties.getAdmins();
        this.elasticIndexer = elasticIndexer;
        this.aktivitetService = aktivitetService;
        this.aktorregisterClient = aktorregisterClient;
        this.tiltakHandler = tiltakHandler;
        this.kopierGR199FraArena = kopierGR199FraArena;
    }

    @PostMapping("/indeks")
    public String indeks() {
        authorizeAdmin();
        RunningJob runningJob = runAsyncJob(elasticIndexer::startIndeksering);
        return "Indeksering startet med jobId " + runningJob.getJobId() + " på pod " + runningJob.getPodName();
    }

    @GetMapping("/hovedindeksering")
    public String hovedindeksering() {
        authorizeAdmin();

        final RunningJob runningJob = runAsyncJob(() -> {
                    try {
                        kopierGR199FraArena.startOppdateringAvYtelser();
                        tiltakHandler.startOppdateringAvTiltakIDatabasen();
                    } finally {
                        elasticIndexer.startIndeksering();
                    }
                }
        );

        return "Hovedindeksering startet med jobId " + runningJob.getJobId() + " på pod " + runningJob.getPodName();
    }

    @GetMapping("/tiltak")
    public String tiltak() {
        authorizeAdmin();
        final RunningJob runningJob = runAsyncJob(() -> tiltakHandler.startOppdateringAvTiltakIDatabasen());
        return "Oppdaterer tiltak i databasen jobId " + runningJob.getJobId() + " på pod " + runningJob.getPodName();
    }


    @GetMapping("/ytelser")
    public String ytelser() {
        authorizeAdmin();
        final RunningJob runningJob = runAsyncJob(() -> kopierGR199FraArena.startOppdateringAvYtelser());
        return "Oppdaterer ytelser med jobId " + runningJob.getJobId() + " på pod " + runningJob.getPodName();
    }

    @PutMapping("/indeks/bruker")
    public void indeks(@RequestBody String fnr) {
        authorizeAdmin();
        elasticIndexer.indekser(Fnr.of(fnr));
    }

    @DeleteMapping("/aktivitet/{id}")
    public void aktivitet(@PathVariable String id) {
        authorizeAdmin();
        aktivitetService.slettAktivitet(id);
    }

    @PostMapping("/aktoerId")
    public String aktoerId(@RequestBody String fnr) {
        authorizeAdmin();
        return aktorregisterClient.hentAktorId(fnr);
    }

    private void authorizeAdmin() {
        final String ident = SubjectHandler.getIdent().orElseThrow();
        if (!admins.contains(ident)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
