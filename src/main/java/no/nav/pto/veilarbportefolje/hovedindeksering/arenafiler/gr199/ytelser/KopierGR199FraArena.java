package no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.gr199.ytelser;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.ArenaFilType;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.FilmottakConfig;
import no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.FilmottakFileUtils;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import org.apache.commons.vfs2.FileObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.FilmottakFileUtils.getLastModifiedTimeInMillis;
import static no.nav.pto.veilarbportefolje.hovedindeksering.arenafiler.FilmottakFileUtils.hoursSinceLastChanged;
import static no.nav.pto.veilarbportefolje.util.StreamUtils.log;

@Slf4j
public class KopierGR199FraArena {

    private final IndekserYtelserHandler indekserHandler;
    private final MetricsClient metricsClient;
    private final EnvironmentProperties environmentProperties;

    @Autowired
    public KopierGR199FraArena(IndekserYtelserHandler indekserHandler, MetricsClient metricsClient, EnvironmentProperties environmentProperties) {
        this.indekserHandler = indekserHandler;
        this.metricsClient = metricsClient;
        this.environmentProperties = environmentProperties;
    }

    public void startOppdateringAvYtelser() {
        log.info("Indeksering: Starter oppdatering av ytelser...");
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
                ArenaFilType.GR_199_YTELSER);
    }

    //Hourly
    @Scheduled(cron = "0 0 * * * *")
    public void sjekkArenaYtelserSistOppdatert() {
        Long millis = getLastModifiedTimeInMillis(lopendeYtelser()).getOrElseThrow(() -> new RuntimeException());
        final long timerSiden = hoursSinceLastChanged(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
        final Event event = new Event("portefolje.arena.fil.ytelser.sist.oppdatert");
        event.addFieldToReport("timerSiden", timerSiden);
        metricsClient.report(event);
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
