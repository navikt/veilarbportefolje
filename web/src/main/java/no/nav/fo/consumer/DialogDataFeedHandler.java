package no.nav.fo.consumer;


import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.OppfolgingUtils.erBrukerUnderOppfolging;

@Slf4j
public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    private final PersistentOppdatering persistentOppdatering;
    private final AktoerService aktoerService;
    private final BrukerRepository brukerRepository;
    private final SolrService solrService;

    @Inject
    public DialogDataFeedHandler(PersistentOppdatering persistentOppdatering,
                                 AktoerService aktoerService,
                                 BrukerRepository brukerRepository,
                                 SolrService solrService) {
        this.persistentOppdatering = persistentOppdatering;
        this.aktoerService = aktoerService;
        this.brukerRepository = brukerRepository;
        this.solrService = solrService;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {
        data.forEach(this::behandleDialogData);
        brukerRepository.updateMetadata("dialogaktor_sist_oppdatert", Date.from(ZonedDateTime.parse(lastEntry).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleDialogData(DialogDataFraFeed dialog) {
        try {
            timed("feed.dialog.objekt",
                    () -> {
                        Try<PersonId> personId = aktoerService.hentPersonidFraAktoerid(AktoerId.of(dialog.aktorId));
                        DialogBrukerOppdatering oppdatering = new DialogBrukerOppdatering(dialog, personId.toJavaOptional().map(PersonId::toString));

                        brukerRepository
                                .retrieveOppfolgingstatus(personId.getOrNull())
                                .onSuccess(
                                        status -> {
                                            if (erBrukerUnderOppfolging(status)) {
                                                persistentOppdatering.lagre(oppdatering);
                                            } else {
                                                persistentOppdatering.hentDataOgLagre(oppdatering);
                                                solrService.slettBruker(personId.getOrNull());
                                                solrService.commit();
                                            }
                                        });
                    },
                    (timer, hasFailed) -> {
                        if (hasFailed) {
                            timer.addTagToReport("aktorhash", DigestUtils.md5Hex(dialog.aktorId).toUpperCase());
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Feil ved behandlig av dialog fra feed med aktorid {}", dialog.aktorId);
        }
    }

    static class DialogBrukerOppdatering implements BrukerOppdatering {
        private final DialogDataFraFeed dialog;
        private Optional<String> personId;

        public DialogBrukerOppdatering(DialogDataFraFeed dialog, Optional<String> personId) {
            this.dialog = dialog;
            this.personId = personId;
        }

        @Override
        public String getPersonid() {
            return personId.get();
        }

        @Override
        public Brukerdata applyTo(Brukerdata bruker) {
            LocalDateTime eldsteVentende = dialog.tidspunktEldsteVentende == null ? null : LocalDateTime.ofInstant(dialog.tidspunktEldsteVentende.toInstant(), ZoneId.systemDefault());
            LocalDateTime eldsteUbehandlede = dialog.tidspunktEldsteUbehandlede == null ? null : LocalDateTime.ofInstant(dialog.tidspunktEldsteUbehandlede.toInstant(), ZoneId.systemDefault());
            return bruker
                    .setVenterPaSvarFraBruker(eldsteVentende)
                    .setVenterPaSvarFraNav(eldsteUbehandlede);
        }
    }
}
