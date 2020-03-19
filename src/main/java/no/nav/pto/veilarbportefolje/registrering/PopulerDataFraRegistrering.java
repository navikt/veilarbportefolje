package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.utils.CollectionUtils;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.registrering.domene.BrukerRegistreringWrapper;
import no.nav.pto.veilarbportefolje.registrering.domene.DinSituasjonSvar;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;


public class PopulerDataFraRegistrering implements Runnable {
    private RegistreringService registreringService;
    private BrukerRepository brukerRepository;
    private VeilarbregistreringClient veilarbregistreringClient;

    static int BATCH_SIZE = 10; //

    public PopulerDataFraRegistrering(RegistreringService registreringService, BrukerRepository brukerRepository, VeilarbregistreringClient veilarbregistreringClient) {
        this.registreringService = registreringService;
        this.brukerRepository = brukerRepository;
        this.veilarbregistreringClient = veilarbregistreringClient;

        JobUtils.runAsyncJob(this);
    }

    public void run() {
        List<OppfolgingsBruker> brukere = brukerRepository.hentAlleBrukereUnderOppfolging().subList(0, 10); // TAR BARA 10 FØRSTE FØR ATT TESTA I Q=
        CollectionUtils.partition(brukere, BATCH_SIZE).forEach(brukerBatch -> {
            brukerBatch.stream().forEach(bruker -> {
                veilarbregistreringClient.hentRegistrering(Fnr.of(bruker.getFnr()))
                        .map(brukerRegistreringData -> mapRegistreringTilArbeidssokerRegistrertEvent(brukerRegistreringData, AktoerId.of(bruker.getAktoer_id())))
                        .onSuccess(arbeidssokerRegistrert -> registreringService.behandleKafkaMelding(arbeidssokerRegistrert));
            });
        });
    }

    private ArbeidssokerRegistrertEvent mapRegistreringTilArbeidssokerRegistrertEvent (BrukerRegistreringWrapper registrering, AktoerId aktoerId) {
        ZonedDateTime opprettetDato = ZonedDateTime.of(registrering.getRegistrering().getOpprettetDato(), ZoneId.systemDefault());
        Optional<DinSituasjonSvar> brukerSituasjon = Optional.ofNullable(registrering.getRegistrering().getBesvarelse().getDinSituasjon());
        return ArbeidssokerRegistrertEvent.newBuilder()
                .setRegistreringOpprettet(opprettetDato.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .setBrukersSituasjon(brukerSituasjon.map(Enum::name).orElse(null))
                .setAktorid(aktoerId.toString())
                .build();
    }


}


