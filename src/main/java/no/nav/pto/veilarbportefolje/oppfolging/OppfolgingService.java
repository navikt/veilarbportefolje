package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.pto.veilarbportefolje.util.RunningJob;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
@Service
public class OppfolgingService {
    private final String veilarboppfolgingUrl;
    private final OkHttpClient client;
    private final BrukerRepository brukerRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final SystemUserTokenProvider systemUserTokenProvider;

    @Autowired
    public OppfolgingService(BrukerRepository brukerRepository, OppfolgingRepository oppfolgingRepository, SystemUserTokenProvider systemUserTokenProvider) {
        this.brukerRepository = brukerRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = UrlUtils.createServiceUrl("veilarboppfolging", "pto", true);
    }

    public OppfolgingService(BrukerRepository brukerRepository, OppfolgingRepository oppfolgingRepository, SystemUserTokenProvider systemUserTokenProvider, String url) {
        this.brukerRepository = brukerRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = url;
    }

    public void lastInnDataPaNytt() {
        RunningJob job = JobUtils.runAsyncJob(
                () -> {
                    String jobId = generateId();
                    MDC.put("jobId", jobId);
                    log.info("Startet oppfolgingsjobb med id: {}", jobId);

                    List<OppfolgingsBruker> oppfolgingsBruker = brukerRepository.hentAlleBrukereUnderOppfolging();
                    oppfolgingsBruker.forEach(this::oppdaterBruker);
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
        Optional<OppfolgingPeriodeDTO> oppfolgingPeriode = hentSisteOppfolgingsPeriode(bruker.getFnr());

        if (oppfolgingPeriode.isPresent()) {
            ZonedDateTime korrektStartDato = oppfolgingPeriode.get().startDato;
            if (korrektStartDato != null) {
                log.info("OppfolgingsJobb: skal bytte startdato fra: {}, til:{} ", bruker.getOppfolging_startdato(), korrektStartDato);
                int rows = oppfolgingRepository.oppdaterStartdato(AktorId.of(bruker.getAktoer_id()), korrektStartDato);
                if (rows != 1) {
                    log.error("OppfolgingsJobb: feil antall rader påvirket ({}) pa bruker: {} ", rows, bruker.getAktoer_id());
                }
            } else {
                log.error("OppfolgingsJobb: startdato fra veialrboppfolging var null pa bruker: {} ", bruker.getAktoer_id());
            }
        } else {
            log.error("OppfolgingsJobb: Fant ikke oppfolgingsperiode for: " + bruker.getAktoer_id());
        }
    }

    @SneakyThrows
    public List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/oppfolging/oppfolgingsperioder?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.getSystemUserToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJsonArray(bodyStr, OppfolgingPeriodeDTO.class))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public Optional<OppfolgingPeriodeDTO> hentSisteOppfolgingsPeriode(String fnr) {
        List<OppfolgingPeriodeDTO> oppfolgingPerioder = hentOppfolgingsperioder(fnr);
        if (oppfolgingPerioder == null) {
            return Optional.empty();
        }

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

}