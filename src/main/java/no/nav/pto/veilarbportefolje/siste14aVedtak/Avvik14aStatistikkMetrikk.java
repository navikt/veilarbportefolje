package no.nav.pto.veilarbportefolje.siste14aVedtak;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.Avvik14aStatistikk;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class Avvik14aStatistikkMetrikk implements MeterBinder {

    private final OpensearchService opensearchService;

    private AtomicLong antallMedInnsatsgruppeUlik;
    private AtomicLong antallMedHovedmaalUlik;
    private AtomicLong antallMedInnsatsgruppeOgHovedmaalUlik;
    private AtomicLong antallMedInnsatsgruppeManglerINyKilde;

    @Autowired
    public Avvik14aStatistikkMetrikk(OpensearchService opensearchService) {
        this.opensearchService = opensearchService;
    }

    @Override
    public void bindTo(@NotNull MeterRegistry meterRegistry) {
        Avvik14aStatistikk avvik14aStatistikk = opensearchService.hentAvvik14aStatistikk();

        this.antallMedInnsatsgruppeUlik = meterRegistry.gauge("veilarbportefolje_avvik14astatistikk_innsatsgruppe_ulik", new AtomicLong(avvik14aStatistikk.antallMedInnsatsgruppeUlik()));
        this.antallMedHovedmaalUlik = meterRegistry.gauge("veilarbportefolje_avvik14astatistikk_hovedmaal_ulik", new AtomicLong(avvik14aStatistikk.antallMedHovedmaalUlik()));
        this.antallMedInnsatsgruppeOgHovedmaalUlik = meterRegistry.gauge("veilarbportefolje_avvik14astatistikk_innsatsgruppe_og_hovedmaal_ulik", new AtomicLong(avvik14aStatistikk.antallMedInnsatsgruppeOgHovedmaalUlik()));
        this.antallMedInnsatsgruppeManglerINyKilde = meterRegistry.gauge("veilarbportefolje_avvik14astatistikk_innsatsgruppe_mangler", new AtomicLong(avvik14aStatistikk.antallMedInnsatsgruppeManglerINyKilde()));
    }

    public void oppdaterMetrikk() {
        log.info("Oppdaterer metrikker for avvik 14 a statistikk");
        Avvik14aStatistikk avvik14aStatistikk = opensearchService.hentAvvik14aStatistikk();

        this.antallMedInnsatsgruppeUlik.set(avvik14aStatistikk.antallMedInnsatsgruppeUlik());
        this.antallMedHovedmaalUlik.set(avvik14aStatistikk.antallMedHovedmaalUlik());
        this.antallMedInnsatsgruppeOgHovedmaalUlik.set(avvik14aStatistikk.antallMedInnsatsgruppeOgHovedmaalUlik());
        this.antallMedInnsatsgruppeManglerINyKilde.set(avvik14aStatistikk.antallMedInnsatsgruppeManglerINyKilde());
    }
}
