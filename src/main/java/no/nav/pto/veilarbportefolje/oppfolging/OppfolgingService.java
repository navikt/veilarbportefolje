package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.JobRunner;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
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
    private final OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    private static int antallBrukereSlettet;

    @Autowired
    public OppfolgingService(BrukerRepository brukerRepository, OppfolgingRepository oppfolgingRepository, OppfolgingAvsluttetService oppfolgingAvsluttetService, SystemUserTokenProvider systemUserTokenProvider, OppfolgingRepositoryV2 oppfolgingRepositoryV2) {
        this.brukerRepository = brukerRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = UrlUtils.createServiceUrl("veilarboppfolging", "pto", true);
    }

    public OppfolgingService(BrukerRepository brukerRepository, OppfolgingRepository oppfolgingRepository, OppfolgingAvsluttetService oppfolgingAvsluttetService, SystemUserTokenProvider systemUserTokenProvider, String url, OppfolgingRepositoryV2 oppfolgingRepositoryV2) {
        this.brukerRepository = brukerRepository;
        this.oppfolgingRepository = oppfolgingRepository;
        this.systemUserTokenProvider = systemUserTokenProvider;
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.oppfolgingRepositoryV2 = oppfolgingRepositoryV2;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = url;
    }

    public void lastInnDataPaNytt() {
        JobRunner.runAsync("OppfolgingSync",
                () -> {
                    antallBrukereSlettet = 0;
                    List<AktorId> oppfolgingsBruker = oppfolgingRepository.hentAlleBrukereUnderOppfolging();
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
                Optional<BrukerOppdatertInformasjon> dbInfoOracle = oppfolgingRepository.hentOppfolgingData(bruker);
                Optional<BrukerOppdatertInformasjon> dbInfoPostgres = oppfolgingRepositoryV2.hentOppfolgingData(bruker);

                oppdaterStartDatoHvisNodvendig(bruker, dbInfoOracle.map(BrukerOppdatertInformasjon::getStartDato).orElse(null), veialrbinfo.getStartDato(), false);
                oppdaterStartDatoHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getStartDato).orElse(null), veialrbinfo.getStartDato(), true);

                oppdaterManuellHvisNodvendig(bruker, dbInfoOracle.map(BrukerOppdatertInformasjon::getManuell).orElse(false), veialrbinfo.isErManuell(), false);
                oppdaterManuellHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getManuell).orElse(false), veialrbinfo.isErManuell(), true);

                oppdaterNyForVeilederHvisNodvendig(bruker, dbInfoOracle.map(BrukerOppdatertInformasjon::getNyForVeileder).orElse(false), veialrbinfo.isNyForVeileder(), false);
                oppdaterNyForVeilederHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getNyForVeileder).orElse(false), veialrbinfo.isNyForVeileder(), true);

                oppdaterVeilederHvisNodvendig(bruker, dbInfoOracle.map(BrukerOppdatertInformasjon::getVeileder).orElse(null), Optional.ofNullable(veialrbinfo.getVeilederId()).map(NavIdent::get).orElse(null), false);
                oppdaterVeilederHvisNodvendig(bruker, dbInfoPostgres.map(BrukerOppdatertInformasjon::getVeileder).orElse(null), Optional.ofNullable(veialrbinfo.getVeilederId()).map(NavIdent::get).orElse(null), true);
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

    private void oppdaterStartDatoHvisNodvendig(AktorId bruker, Timestamp startFraDb, ZonedDateTime korrektStartDato, boolean postgres) {
        if (korrektStartDato == null) {
            log.warn("OppfolgingsJobb: startdato fra veilarboppfolging var null pa bruker: {} ", bruker);
            return;
        }
        ZonedDateTime zonedDbVerdi = Optional.ofNullable(startFraDb).map(timestamp -> ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"))).orElse(null);

        if (zonedDbVerdi != null && korrektStartDato.isEqual(zonedDbVerdi)) {
            return;
        }
        if (postgres) {
            log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte startdato fra: {}, til:{} ", bruker, zonedDbVerdi, korrektStartDato);
            oppfolgingRepositoryV2.settStartdato(bruker, korrektStartDato);
        } else {
            log.info("OppfolgingsJobb: aktoer: {} skal bytte startdato fra: {}, til:{} ", bruker, zonedDbVerdi, korrektStartDato);
            oppfolgingRepository.oppdaterStartdato(bruker, korrektStartDato);
        }
    }

    private void oppdaterManuellHvisNodvendig(AktorId bruker, boolean manuellDb, boolean korrektManuell, boolean postgres) {
        if (manuellDb == korrektManuell) {
            return;
        }

        if (postgres) {
            log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte manuell fra: {}, til:{} ", bruker, manuellDb, korrektManuell);
            oppfolgingRepositoryV2.settManuellStatus(bruker, korrektManuell);
        } else {
            log.info("OppfolgingsJobb: aktoer: {} skal bytte manuell fra: {}, til:{} ", bruker, manuellDb, korrektManuell);
            oppfolgingRepository.settManuellStatus(bruker, korrektManuell);
        }
    }


    private void oppdaterNyForVeilederHvisNodvendig(AktorId bruker, boolean nyForVeilederDb, boolean korrektNyForVeileder, boolean postgres) {
        if (nyForVeilederDb == korrektNyForVeileder) {
            return;
        }

        if (postgres) {
            log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte nyForVeileder fra: {}, til:{} ", bruker, nyForVeilederDb, korrektNyForVeileder);
            oppfolgingRepositoryV2.settNyForVeileder(bruker, korrektNyForVeileder);
        } else {
            log.info("OppfolgingsJobb: aktoer: {} skal bytte nyForVeileder fra: {}, til:{} ", bruker, nyForVeilederDb, korrektNyForVeileder);
            oppfolgingRepository.settNyForVeileder(bruker, korrektNyForVeileder);
        }
    }

    private void oppdaterVeilederHvisNodvendig(AktorId bruker, String veilederDb, String korrektVeileder, boolean postgres) {
        if (veilederDb == null) {
            if (korrektVeileder == null) {
                return;
            }
        } else if (veilederDb.equals(korrektVeileder)) {
            return;
        }

        if (postgres) {
            log.info("(Postgres) OppfolgingsJobb: aktoer: {} skal bytte veileder fra: {}, til:{} ", bruker, veilederDb, korrektVeileder);
            oppfolgingRepositoryV2.settVeileder(bruker, VeilederId.of(korrektVeileder));
        } else {
            log.info("OppfolgingsJobb: aktoer: {} skal bytte veileder fra: {}, til:{} ", bruker, veilederDb, korrektVeileder);
            oppfolgingRepository.settVeileder(bruker, VeilederId.of(korrektVeileder));
        }
    }

    private Veilarbportefoljeinfo hentVeilarbData(AktorId aktoer) throws RuntimeException, IOException {
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/admin/hentVeilarbinfo/bruker?aktorId=" + aktoer))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.getSystemUserToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, Veilarbportefoljeinfo.class))
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }
}