package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.krr.KrrRepository;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepository;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepository;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public PersistentOppdatering persistentOppdatering() {
        return new PersistentOppdatering();
    }

    @Bean
    public ArbeidslisteService arbeidslisteService() {
        return new ArbeidslisteService();
    }

    @Bean
    public AktivitetService aktivitetService(AktoerService aktoerService, AktivitetDAO aktivitetDAO, PersistentOppdatering persistentOppdatering) {
        return new AktivitetService(aktoerService, aktivitetDAO, persistentOppdatering);
    }

    @Bean
    public TiltakService tiltakService() {
        return new TiltakService();
    }

    @Bean
    public KrrService krrService(KrrRepository krrRepository, DigitalKontaktinformasjonV1 dkif) {
        return new KrrService(krrRepository, dkif);
    }

    @Bean
    public DialogService dialogService(DialogFeedRepository dialogFeedRepository, ElasticIndexer elasticIndexer) {
        return new DialogService(dialogFeedRepository, elasticIndexer);
    }

    @Bean
    public VedtakService vedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        return new VedtakService(vedtakStatusRepository, elasticIndexer, aktoerService);
    }

    @Bean
    public RegistreringService registreringService(RegistreringRepository registreringRepository, ElasticIndexer elasticIndexer) {
        return new RegistreringService(registreringRepository, elasticIndexer);
    }
}
