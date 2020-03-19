package no.nav.pto.veilarbportefolje.registrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.utils.CollectionUtils;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.registrering.domene.BrukerRegistreringWrapper;
import no.nav.pto.veilarbportefolje.registrering.domene.DinSituasjonSvar;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PopulerDataFraRegistrering implements Runnable {
    private RegistreringService registreringService;
    private BrukerRepository brukerRepository;
    private VeilarbregistreringClient veilarbregistreringClient;

    static int BATCH_SIZE = 10; //TODO SETT TILL 1000 SOM ARBEIDSSOKER

    public PopulerDataFraRegistrering(RegistreringService registreringService, BrukerRepository brukerRepository, VeilarbregistreringClient veilarbregistreringClient) {
        this.registreringService = registreringService;
        this.brukerRepository = brukerRepository;
        this.veilarbregistreringClient = veilarbregistreringClient;

        JobUtils.runAsyncJob(this);
    }

    public void run() {
        List<OppfolgingsBruker> brukere = brukerRepository.hentAlleBrukereUnderOppfolging().subList(0, 10); //TODO TAR BARA 10 FØRSTE FØR ATT TESTA I Q0 FJERN FJERN FJERN!!!
        CollectionUtils.partition(brukere, BATCH_SIZE).forEach(brukerBatch -> {
            brukerBatch.stream().forEach(bruker -> {
                veilarbregistreringClient.hentRegistrering(Fnr.of(bruker.getFnr()))
                        .map(brukerRegistreringData -> mapRegistreringTilArbeidssokerRegistrertEvent(brukerRegistreringData, AktoerId.of(bruker.getAktoer_id())))
                        .onSuccess(arbeidssokerRegistrert -> registreringService.behandleKafkaMelding(arbeidssokerRegistrert))
                        .onFailure(error -> log.warn(String.format("Feilede att registreringsdata for aktorId %s med føljande fel : %s ", bruker.getAktoer_id(), error)));
            });
        });
    }

    private ArbeidssokerRegistrertEvent mapRegistreringTilArbeidssokerRegistrertEvent (BrukerRegistreringWrapper registrering, AktoerId aktoerId) {
        Optional<DinSituasjonSvar> brukerSituasjon = Optional.ofNullable(registrering.getRegistrering().getBesvarelse().getDinSituasjon());
        return ArbeidssokerRegistrertEvent.newBuilder()
                .setRegistreringOpprettet(null)
                .setBrukersSituasjon(brukerSituasjon.map(Enum::name).orElse(null))
                .setAktorid(aktoerId.toString())
                .build();
    }


}


