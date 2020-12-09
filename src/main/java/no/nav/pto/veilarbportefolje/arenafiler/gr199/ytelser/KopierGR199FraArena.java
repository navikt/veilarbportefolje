package no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser;

import io.micrometer.core.instrument.Gauge;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenafiler.ArenaFilType;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import org.apache.commons.vfs2.FileObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.pto.veilarbportefolje.arenafiler.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.pto.veilarbportefolje.elastic.MetricsReporter.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    private final AktivitetService aktivitetService;
    private final IndekserYtelserHandler indekserHandler;
    private final MetricsClient metricsClient;
    private final EnvironmentProperties environmentProperties;

    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler, AktivitetService aktivitetService, MetricsClient metricsClient, EnvironmentProperties environmentProperties) {
        this.indekserHandler = indekserHandler;
        this.aktivitetService = aktivitetService;
        this.metricsClient = metricsClient;
        this.environmentProperties = environmentProperties;
        Gauge.builder("portefolje_arena_fil_ytelser_sist_oppdatert", this::sjekkArenaYtelserSistOppdatert).register(getMeterRegistry());

    }

    public void startOppdateringAvYtelser() {
        log.info("Indeksering: Starter oppdatering av ytelser...");
        aktivitetService.tryUtledOgLagreAlleAktivitetstatuser(); //TODO VARFÖR BEHÖVER MAN GÖRA DETTA VID INLÄSNING AV YTELSER?
        this.hentYtelserFil()
                .onFailure(log(log, "Kunne ikke hente ut fil med ytelser via nfs"))
                .flatMap(FilmottakFileUtils::unmarshallLoependeYtelserFil)
                .onFailure(log(log, "Unmarshalling av ytelsesfil feilet"))
                .andThen(indekserHandler::lagreYtelser)
                .onFailure(log(log, "Hovedindeksering feilet"));

        log.info("Indeksering: Fullført oppdatering av ytelser");
    }

    public FilmottakConfig.SftpConfig lopendeYtelser() {
        return new FilmottakConfig.SftpConfig(
                environmentProperties.getArenaLoependeYtelserUrl(),
                environmentProperties.getArenaFilmottakSFTPUsername(),
                environmentProperties.getArenaFilmottakSFTPPassword(),
                ArenaFilType.GR_202_YTELSER);
    }

    public long sjekkArenaYtelserSistOppdatert() {
        Long millis = getLastModifiedTimeInMillis(lopendeYtelser()).getOrElseThrow(() -> new RuntimeException());
        return hoursSinceLastChanged(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("Europe/Oslo")));
    }

    public Try<FileObject> hentYtelserFil() {
        return FilmottakFileUtils.hentFil(lopendeYtelser());
    }

    public HealthCheckResult sftpLopendeYtelserPing() {
        Try<FileObject> result = this.hentYtelserFil();
        if (result.isFailure()) {
            return HealthCheckResult.unhealthy("Klarte ikke å hente fil for ytelser");
        }

        return HealthCheckResult.healthy();
    }

}
