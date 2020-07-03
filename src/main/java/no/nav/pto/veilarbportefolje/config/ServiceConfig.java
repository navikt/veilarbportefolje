package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.aktviteter.KafkaAktivitetService;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakService;
import no.nav.pto.veilarbportefolje.cv.CvRepository;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.krr.KrrRepository;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.profilering.ProfileringRepository;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringRepository;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.service.NavKontorService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakStatusRepository;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.getAlias;

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
    public RegistreringService registreringService(RegistreringRepository registreringRepository) {
        return new RegistreringService(registreringRepository);
    }

    @Bean
    public NavKontorService navKontorService(BrukerRepository brukerRepository) {
        return new NavKontorService(brukerRepository);
    }

    @Bean
    public OppfolgingService oppfolgingService(OppfolgingRepository oppfolgingRepository, ElasticIndexer elasticIndexer, VeilederService veilederService, NavKontorService navKontorService, UnleashService unleashService, AktoerService aktoerService) {
        return new OppfolgingService(oppfolgingRepository, elasticIndexer, veilederService, navKontorService, arbeidslisteService(), unleashService, aktoerService);
    }

    @Bean
    public KafkaAktivitetService kafkaAktivitetService(AktivitetService aktivitetService, UnleashService unleashService) {
        return new KafkaAktivitetService(aktivitetService, unleashService);
    }

    @Bean
    public CvService cvService(ElasticServiceV2 elasticServiceV2, AktoerService aktoerService, CvRepository cvRepository) {
        return new CvService(elasticServiceV2, aktoerService, cvRepository);
    }

    @Bean
    public ProfileringService profileringService(ProfileringRepository profileringRepository) {
        return new ProfileringService(profileringRepository);
    }

    @Bean
    public ElasticServiceV2 elasticServiceV2(RestHighLevelClient restHighLevelClient) {
        return new ElasticServiceV2(restHighLevelClient, getAlias());
    }
}
