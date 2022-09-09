package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.JobRunner;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.response.Veilarbportefoljeinfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
@Service
public class OppfolgingService {
    private final String veilarboppfolgingUrl;
    private final OkHttpClient client;
    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private final AktorClient aktorClient;
    private final Supplier<String> systemUserTokenProvider;

    private static int antallBrukereSlettet;

    @Autowired
    public OppfolgingService(OppfolgingAvsluttetService oppfolgingAvsluttetService, OppfolgingRepositoryV2 oppfolgingRepositoryV2, AktorClient aktorClient, AzureAdMachineToMachineTokenClient tokenClient) {
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.aktorClient = aktorClient;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = UrlUtils.createServiceUrl("veilarboppfolging", "pto", true);
        systemUserTokenProvider = () -> tokenClient.createMachineToMachineToken(String.format("api://%s-fss.pto.veilarboppfolging/.default", (EnvironmentUtils.isProduction().orElseThrow()) ? "prod" : "dev"));

    }

    public OppfolgingService(OppfolgingAvsluttetService oppfolgingAvsluttetService, String url, OppfolgingRepositoryV2 oppfolgingRepositoryV2, AktorClient aktorClient,  Supplier<String> systemUserTokenProvider) {
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.aktorClient = aktorClient;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = url;
    }

    public void lastInnDataPaNytt() {
        JobRunner.runAsync("OppfolgingSync",
                () -> {
                    antallBrukereSlettet = 0;
                    List<AktorId> oppfolgingsBruker = oppfolgingRepositoryV2.hentAlleBrukereUnderOppfolging();
                    oppfolgingsBruker.forEach(this::oppdaterBruker);

                    log.info("OppfolgingsJobb: oppdaterte informasjon pa: {} brukere der av: {} ble slettet", oppfolgingsBruker.size(), antallBrukereSlettet);
                });
    }

    public void oppdaterBruker(AktorId bruker) {
        if (bruker == null) {
            return;
        }

        try {
            Veilarbportefoljeinfo veialrbinfo = hentVeilarbData(bruker);
            if (veialrbinfo.isErUnderOppfolging()) {
                Optional<BrukerOppdatertInformasjon> dbInfoPostgres = oppfolgingRepositoryV2.hentOppfolgingData(bruker);

                oppdaterStartDatoHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getStartDato).orElse(null), veialrbinfo.getStartDato());
                oppdaterManuellHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getManuell).orElse(false), veialrbinfo.isErManuell());
                oppdaterNyForVeilederHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getNyForVeileder).orElse(false), veialrbinfo.isNyForVeileder());
                oppdaterVeilederHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getVeileder).orElse(null), Optional.ofNullable(veialrbinfo.getVeilederId()).map(NavIdent::get).orElse(null));
            } else {
                log.info("OppfolgingsJobb: bruker er ikke under oppfolging, aktoer: " + bruker);
                oppfolgingAvsluttetService.avsluttOppfolging(bruker);
                antallBrukereSlettet++;
            }
        } catch (RuntimeException e) {
            log.error("RuntimeException i OppfolgingsJobb for bruker {}", bruker);
            log.error("RuntimeException i OppfolgingsJobb", e);
        } catch (Exception e) {
            log.error("Exception i OppfolgingsJobb for bruker {}", bruker);
            log.error("Exception i OppfolgingsJobb", e);
        }
    }

    private void oppdaterStartDatoHvisNodvendig(AktorId bruker, Timestamp startFraDb, ZonedDateTime korrektStartDato) {
        if (korrektStartDato == null) {
            log.warn("OppfolgingsJobb: startdato fra veilarboppfolging var null pa bruker: {} ", bruker);
            return;
        }
        ZonedDateTime zonedDbVerdi = Optional.ofNullable(startFraDb).map(timestamp -> ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"))).orElse(null);

        if (zonedDbVerdi != null && korrektStartDato.isEqual(zonedDbVerdi)) {
            return;
        }
        log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte startdato fra: {}, til:{} ", bruker, zonedDbVerdi, korrektStartDato);
        oppfolgingRepositoryV2.settStartdato(bruker, korrektStartDato);
    }

    private void oppdaterManuellHvisNodvendig(AktorId bruker, boolean manuellDb, boolean korrektManuell) {
        if (manuellDb == korrektManuell) {
            return;
        }

        log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte manuell fra: {}, til:{} ", bruker, manuellDb, korrektManuell);
        oppfolgingRepositoryV2.settManuellStatus(bruker, korrektManuell);
    }


    private void oppdaterNyForVeilederHvisNodvendig(AktorId bruker, boolean nyForVeilederDb, boolean korrektNyForVeileder) {
        if (nyForVeilederDb == korrektNyForVeileder) {
            return;
        }

        log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte nyForVeileder fra: {}, til:{} ", bruker, nyForVeilederDb, korrektNyForVeileder);
        oppfolgingRepositoryV2.settNyForVeileder(bruker, korrektNyForVeileder);

    }

    private void oppdaterVeilederHvisNodvendig(AktorId bruker, String veilederDb, String korrektVeileder) {
        if (veilederDb == null) {
            if (korrektVeileder == null) {
                return;
            }
        } else if (veilederDb.equals(korrektVeileder)) {
            return;
        }

        log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte veileder fra: {}, til:{} ", bruker, veilederDb, korrektVeileder);
        oppfolgingRepositoryV2.settVeileder(bruker, VeilederId.of(korrektVeileder));

    }

    private Veilarbportefoljeinfo hentVeilarbData(AktorId aktoer) throws RuntimeException, IOException {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/admin/hentVeilarbinfo/bruker?aktorId=" + aktoer))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, Veilarbportefoljeinfo.class))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }

    @SneakyThrows
    public boolean hentUnderOppfolging(AktorId aktorId) {
        Fnr fnr = aktorClient.hentFnr(aktorId);
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/v2/oppfolging?fnr=" + fnr))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, UnderOppfolgingV2Response.class))
                    .map(r -> r.erUnderOppfolging)
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }
}