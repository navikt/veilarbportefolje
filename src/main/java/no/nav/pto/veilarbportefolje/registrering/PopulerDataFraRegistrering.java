package no.nav.pto.veilarbportefolje.registrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.internal.AuthorizationUtils;
import no.nav.pto.veilarbportefolje.registrering.domene.BrukerRegistreringWrapper;
import no.nav.pto.veilarbportefolje.registrering.domene.DinSituasjonSvar;
import no.nav.pto.veilarbportefolje.registrering.domene.HentRegistreringDTO;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PopulerDataFraRegistrering extends HttpServlet {
    private RegistreringService registreringService;
    private BrukerRepository brukerRepository;
    private VeilarbregistreringClient veilarbregistreringClient;

    public PopulerDataFraRegistrering(RegistreringService registreringService, BrukerRepository brukerRepository, VeilarbregistreringClient veilarbregistreringClient) {
        this.registreringService = registreringService;
        this.brukerRepository = brukerRepository;
        this.veilarbregistreringClient = veilarbregistreringClient;
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (AuthorizationUtils.isBasicAuthAuthorized(req)) {
            Integer fra = Integer.parseInt(req.getParameter("fra"));
            Integer til = Integer.parseInt(req.getParameter("til"));
            populerMedBrukerRegistrering(fra, til);
            resp.setStatus(200);
        }
        else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

    public void populerMedBrukerRegistrering(Integer fra, Integer til) {
        long t0 = System.currentTimeMillis();

        List<HentRegistreringDTO> subList = brukerRepository.hentAlleBrukereUnderOppfolgingRegistrering(fra, til);

        subList.forEach(bruker -> veilarbregistreringClient.hentRegistrering(bruker.getFnr())
                .map(brukerRegistreringData -> {
                    if(brukerRegistreringData != null) {
                        log.info("Hentet registreringsdata for brukere med aktorId" + bruker.getAktoerId().toString());
                        return mapRegistreringTilArbeidssokerRegistrertEvent(brukerRegistreringData, bruker.getAktoerId());
                    }
                    else {
                        throw new Error("Brukaren hade ikke registrert sig");
                    }
                })
                .onSuccess(arbeidssokerRegistrert -> registreringService.behandleKafkaMelding(arbeidssokerRegistrert))
                .onFailure(error -> log.warn(String.format("Feilede att registreringsdata for aktorId %s med føljande fel : %s ", bruker.getAktoerId(), error.getMessage()))));

        long t1 = System.currentTimeMillis();
        long time = t1 - t0;

        int antall = til-fra;
        log.info(String.format("Hentning av brukerregistreringdata før %s brukere tokk %s ", antall,  time));
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


