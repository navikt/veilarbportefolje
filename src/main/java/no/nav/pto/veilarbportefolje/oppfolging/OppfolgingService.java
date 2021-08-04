package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.JobRunner;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
@Service
public class OppfolgingService {
    private final String veilarboppfolgingUrl;
    private final OkHttpClient client;
    private final BrukerRepository brukerRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final SystemUserTokenProvider systemUserTokenProvider;

    private static int antallBrukereSlettet;

    @Autowired
    public OppfolgingService(BrukerRepository brukerRepository, OppfolgingRepository oppfolgingRepository, OppfolgingAvsluttetService oppfolgingAvsluttetService, SystemUserTokenProvider systemUserTokenProvider) {
        this.brukerRepository = brukerRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = UrlUtils.createServiceUrl("veilarboppfolging", "pto", true);
    }

    public OppfolgingService(BrukerRepository brukerRepository, OppfolgingRepository oppfolgingRepository, OppfolgingAvsluttetService oppfolgingAvsluttetService, SystemUserTokenProvider systemUserTokenProvider, String url) {
        this.brukerRepository = brukerRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = url;
    }

    public void lastInnDataPaNytt() {
        JobRunner.runAsync("OppfolgingSync",
                () -> {
                    antallBrukereSlettet = 0;
                    List<OppfolgingsBruker> oppfolgingsBruker = brukerRepository.hentAlleBrukereUnderOppfolging();
                    oppfolgingsBruker.forEach(this::oppdaterBruker);

                    log.info("OppfolgingsJobb: oppdaterte informasjon pa: {} brukere der av: {} ble slettet", oppfolgingsBruker.size(), antallBrukereSlettet);
                });
    }

    public void oppdaterBruker(OppfolgingsBruker bruker) {
        if (bruker.getAktoer_id() == null) {
            return;
        }
        if (bruker.getFnr() == null) {
            log.error("Fnr var null pa bruker: " + bruker.getAktoer_id());
            return;
        }

        try {
            Optional<OppfolgingPeriodeDTO> oppfolgingPeriode = hentSisteOppfolgingsPeriode(bruker.getFnr());
            if (oppfolgingPeriode.isPresent()) {
                oppdaterStartDato(bruker, oppfolgingPeriode.get().startDato);
                avsluttOppfolgingHvisNodvendig(bruker, oppfolgingPeriode.get());
            } else {
                log.info("OppfolgingsJobb: Fant ikke oppfolgingsperiode for: " + bruker.getAktoer_id());
                oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(bruker.getAktoer_id()));
                antallBrukereSlettet++;
            }
        } catch (RuntimeException e) {
            log.error("RuntimeException i OppfolgingsJobb for bruker {}", bruker.getAktoer_id());
            log.error("RuntimeException i OppfolgingsJobb", e);
        } catch (Exception e) {
            log.error("Exception i OppfolgingsJobb for bruker {}", bruker.getAktoer_id());
            log.error("Exception i OppfolgingsJobb", e);
        }
    }

    private void oppdaterStartDato(OppfolgingsBruker bruker, ZonedDateTime korrektStartDato) {
        if (korrektStartDato == null) {
            log.warn("OppfolgingsJobb: startdato fra veilarboppfolging var null pa bruker: {} ", bruker.getAktoer_id());
            return;
        }
        if (bruker.getOppfolging_startdato() != null && korrektStartDato.isEqual(ZonedDateTime.parse(bruker.getOppfolging_startdato()))) {
            return;
        }
        log.info("OppfolgingsJobb: aktoer: {} skal bytte startdato fra: {}, til:{} ", bruker.getAktoer_id(), bruker.getOppfolging_startdato(), korrektStartDato);
        int rows = oppfolgingRepository.oppdaterStartdato(AktorId.of(bruker.getAktoer_id()), korrektStartDato);
        if (rows != 1) {
            log.error("OppfolgingsJobb: feil antall rader påvirket ({}) pa bruker: {} ", rows, bruker.getAktoer_id());
        }
    }

    private void avsluttOppfolgingHvisNodvendig(OppfolgingsBruker bruker, OppfolgingPeriodeDTO oppfolgingPeriode) {
        if (!underOppfolging(oppfolgingPeriode)) {
            log.info("OppfolgingsJobb: Oppfolging avsluttet for:" + bruker.getAktoer_id());
            oppfolgingAvsluttetService.avsluttOppfolging(AktorId.of(bruker.getAktoer_id()));
            antallBrukereSlettet++;
        }
    }

    private List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(String fnr) throws RuntimeException, IOException {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/oppfolging/oppfolgingsperioder?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.getSystemUserToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJsonArray(bodyStr, OppfolgingPeriodeDTO.class))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }

    private Optional<OppfolgingPeriodeDTO> hentSisteOppfolgingsPeriode(String fnr) throws RuntimeException, IOException {
        List<OppfolgingPeriodeDTO> oppfolgingPerioder = hentOppfolgingsperioder(fnr);

        return oppfolgingPerioder.stream().min((o1, o2) -> {
            if (o1.sluttDato == null) {
                return -1;
            }

            if (o2.sluttDato == null) {
                return 1;
            }

            if (o1.sluttDato.isAfter(o2.sluttDato)) {
                return -1;
            }

            if (o1.sluttDato.isBefore(o2.sluttDato)) {
                return 1;
            }

            return 0;
        });
    }

    private boolean underOppfolging(OppfolgingPeriodeDTO oppfolgingPeriode) {
        return oppfolgingPeriode.sluttDato == null;
    }
}